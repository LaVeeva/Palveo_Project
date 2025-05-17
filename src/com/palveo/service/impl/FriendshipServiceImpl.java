package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.palveo.dao.FriendshipDao;
import com.palveo.dao.UserDao;
import com.palveo.dao.impl.FriendshipDaoImpl;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Friendship;
import com.palveo.model.User;
import com.palveo.service.FriendshipService;
import com.palveo.service.exception.FriendshipOperationException;
import com.palveo.service.exception.UserNotFoundException;

public class FriendshipServiceImpl implements FriendshipService {

    private FriendshipDao friendshipDao;
    private UserDao userDao;

    public FriendshipServiceImpl() {
        this.friendshipDao = new FriendshipDaoImpl();
        this.userDao = new UserDaoImpl();
    }

    public FriendshipServiceImpl(FriendshipDao friendshipDao, UserDao userDao) {
        this.friendshipDao = friendshipDao;
        this.userDao = userDao;
    }

    private User getUserOrThrow(int userId, String context)
            throws UserNotFoundException, FriendshipOperationException {
        try {
            return userDao.findById(userId).orElseThrow(() -> new UserNotFoundException(
                    "User not found with ID: " + userId + " (" + context + ")"));
        } catch (SQLException e) {
            throw new FriendshipOperationException(
                    "Database error fetching user " + userId + " (" + context + ")", e);
        }
    }

    private Friendship getExistingFriendshipOrThrow(User userA, User userB, String errorMessage, Connection conn)
            throws FriendshipOperationException {
        try {
            return friendshipDao.findFriendship(userA.getId(), userB.getId()) 
                    .orElseThrow(() -> new FriendshipOperationException(errorMessage));
        } catch (SQLException e) {
            throw new FriendshipOperationException("Database error checking existing friendship.",
                    e);
        }
    }

    @Override
    public Friendship sendFriendRequest(User requester, User recipient)
            throws FriendshipOperationException, UserNotFoundException {
        if (requester == null || recipient == null)
            throw new FriendshipOperationException("Requester and recipient cannot be null.");
        if (requester.getId() == recipient.getId())
            throw new FriendshipOperationException("Cannot send a friend request to yourself.");

        getUserOrThrow(requester.getId(), "sending friend request");
        getUserOrThrow(recipient.getId(), "sending friend request to recipient");

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Optional<Friendship> existingFriendshipOpt =
                    friendshipDao.findFriendship(requester.getId(), recipient.getId());
            if (existingFriendshipOpt.isPresent()) {
                Friendship existing = existingFriendshipOpt.get();
                switch (existing.getStatus()) {
                    case ACCEPTED:
                        throw new FriendshipOperationException(
                                "You are already friends with " + recipient.getUsername());
                    case PENDING:
                        if (existing.getActionUserId() == requester.getId())
                            throw new FriendshipOperationException(
                                    "You have already sent a request to "
                                            + recipient.getUsername());
                        else
                            throw new FriendshipOperationException(recipient.getUsername()
                                    + " has already sent you a request. Please respond to it.");
                    case BLOCKED:
                        if (existing.getActionUserId() == requester.getId())
                            throw new FriendshipOperationException(
                                    "You have blocked " + recipient.getUsername()
                                            + ". Unblock first to send a request.");
                        else
                            throw new FriendshipOperationException(
                                    recipient.getUsername() + " has blocked you.");
                    case DECLINED:
                        friendshipDao.deleteFriendship(requester.getId(), recipient.getId(), conn);
                        break;
                }
            }

            Friendship newRequest = new Friendship(requester.getId(), recipient.getId(),
                    Friendship.FriendshipStatus.PENDING, requester.getId());
            int friendshipId = friendshipDao.saveNewRequest(newRequest, conn);
            
            newRequest.setFriendshipId(friendshipId);
            DatabaseConnection.commitTransaction();
            return newRequest;

        } catch (DuplicateKeyException dke) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (sendFriendRequest DupKey): " + re.getMessage());}
             throw new FriendshipOperationException("A friendship request or record already exists between these users.", dke);
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (sendFriendRequest SQL): " + re.getMessage());}
            throw new FriendshipOperationException("Database error sending friend request.", e);
        } catch (FriendshipOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (sendFriendRequest Op/User): " + re.getMessage());}
            throw e;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("FriendshipServiceImpl: Error ensuring transaction is rolled back in finally (sendFriendRequest): " + se.getMessage());}
            }
        }
    }

    @Override
    public Friendship acceptFriendRequest(User acceptor, User requester)
            throws FriendshipOperationException, UserNotFoundException {
        if (acceptor == null || requester == null)
            throw new FriendshipOperationException("Acceptor and requester cannot be null.");
        getUserOrThrow(acceptor.getId(), "accepting friend request");
        getUserOrThrow(requester.getId(), "accepting friend request from requester");
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            
            Friendship friendship = getExistingFriendshipOrThrow(acceptor, requester,
                "No pending friend request found from " + requester.getUsername(), conn);


            if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
                throw new FriendshipOperationException(
                        "This friend request is not pending. Current status: "
                                + friendship.getStatus());
            }
            if (friendship.getActionUserId() == acceptor.getId()) {
                throw new FriendshipOperationException("You cannot accept a request you sent.");
            }
            if (!((friendship.getUserOneId() == acceptor.getId()
                    && friendship.getUserTwoId() == requester.getId())
                    || (friendship.getUserOneId() == requester.getId()
                            && friendship.getUserTwoId() == acceptor.getId()))) {
                throw new FriendshipOperationException("Friendship record mismatch.");
            }
            
            boolean success = friendshipDao.updateStatus(acceptor.getId(), requester.getId(),
                    Friendship.FriendshipStatus.ACCEPTED, acceptor.getId(), conn);
            if (!success)
                throw new FriendshipOperationException(
                        "Failed to update friend request status to ACCEPTED.");
            
            friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
            friendship.setActionUserId(acceptor.getId());
            friendship.setUpdatedAt(LocalDateTime.now());
            DatabaseConnection.commitTransaction();
            return friendship;
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (acceptFriendRequest SQL): " + re.getMessage());}
            throw new FriendshipOperationException("Database error accepting friend request.", e);
        } catch (FriendshipOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (acceptFriendRequest Op/User): " + re.getMessage());}
            throw e;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("FriendshipServiceImpl: Error ensuring transaction is rolled back in finally (acceptFriendRequest): " + se.getMessage());}
            }
        }
    }

    @Override
    public Friendship rejectFriendRequest(User rejector, User requester)
            throws FriendshipOperationException, UserNotFoundException {
        if (rejector == null || requester == null)
            throw new FriendshipOperationException("Rejector and requester cannot be null.");
        getUserOrThrow(rejector.getId(), "rejecting friend request");
        getUserOrThrow(requester.getId(), "rejecting friend request from requester");

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Friendship friendship = getExistingFriendshipOrThrow(rejector, requester,
                "No pending friend request found from " + requester.getUsername() + " to reject.", conn);

            if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
                throw new FriendshipOperationException(
                        "This friend request is not pending. Current status: "
                                + friendship.getStatus());
            }
            if (friendship.getActionUserId() == rejector.getId()) {
                throw new FriendshipOperationException(
                        "You cannot reject a request you sent; cancel it instead if needed.");
            }

            boolean success = friendshipDao.updateStatus(rejector.getId(), requester.getId(),
                    Friendship.FriendshipStatus.DECLINED, rejector.getId(), conn);
            if (!success)
                throw new FriendshipOperationException(
                        "Failed to update friend request status to DECLINED.");
            
            friendship.setStatus(Friendship.FriendshipStatus.DECLINED);
            friendship.setActionUserId(rejector.getId());
            friendship.setUpdatedAt(LocalDateTime.now());
            DatabaseConnection.commitTransaction();
            return friendship;
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (rejectFriendRequest SQL): " + re.getMessage());}
            throw new FriendshipOperationException("Database error rejecting friend request.", e);
        } catch (FriendshipOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (rejectFriendRequest Op/User): " + re.getMessage());}
            throw e;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("FriendshipServiceImpl: Error ensuring transaction is rolled back in finally (rejectFriendRequest): " + se.getMessage());}
            }
        }
    }

    @Override
    public void removeFriend(User remover, User friendToRemove)
            throws FriendshipOperationException, UserNotFoundException {
        if (remover == null || friendToRemove == null)
            throw new FriendshipOperationException("Remover and friend to remove cannot be null.");
        getUserOrThrow(remover.getId(), "removing friend");
        getUserOrThrow(friendToRemove.getId(), "removing friend (target)");

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Friendship friendship = getExistingFriendshipOrThrow(remover, friendToRemove,
                "No friendship record found with " + friendToRemove.getUsername(), conn);

            if (friendship.getStatus() != Friendship.FriendshipStatus.ACCEPTED) {
                throw new FriendshipOperationException("You are not currently friends with "
                        + friendToRemove.getUsername() + ". Status: " + friendship.getStatus());
            }
            
            boolean success =
                    friendshipDao.deleteFriendship(remover.getId(), friendToRemove.getId(), conn);
            if (!success)
                throw new FriendshipOperationException(
                        "Failed to remove friend " + friendToRemove.getUsername());
            DatabaseConnection.commitTransaction();
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (removeFriend SQL): " + re.getMessage());}
            throw new FriendshipOperationException("Database error removing friend.", e);
        } catch (FriendshipOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (removeFriend Op/User): " + re.getMessage());}
            throw e;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("FriendshipServiceImpl: Error ensuring transaction is rolled back in finally (removeFriend): " + se.getMessage());}
            }
        }
    }

    @Override
    public Friendship blockUser(User blocker, User userToBlock)
            throws FriendshipOperationException, UserNotFoundException {
        if (blocker == null || userToBlock == null)
            throw new FriendshipOperationException("Blocker and user to block cannot be null.");
        if (blocker.getId() == userToBlock.getId())
            throw new FriendshipOperationException("Cannot block yourself.");
        getUserOrThrow(blocker.getId(), "blocking user");
        getUserOrThrow(userToBlock.getId(), "blocking user (target)");

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Optional<Friendship> existingFriendshipOpt =
                    friendshipDao.findFriendship(blocker.getId(), userToBlock.getId());
            if (existingFriendshipOpt.isPresent()) {
                Friendship existing = existingFriendshipOpt.get();
                if (existing.getStatus() == Friendship.FriendshipStatus.BLOCKED
                        && existing.getActionUserId() == blocker.getId()) {
                    DatabaseConnection.commitTransaction(); 
                    return existing;
                }
                boolean success = friendshipDao.updateStatus(blocker.getId(), userToBlock.getId(),
                        Friendship.FriendshipStatus.BLOCKED, blocker.getId(), conn);
                if (!success)
                    throw new FriendshipOperationException(
                            "Failed to update status to BLOCKED for user "
                                    + userToBlock.getUsername());
                existing.setStatus(Friendship.FriendshipStatus.BLOCKED);
                existing.setActionUserId(blocker.getId());
                existing.setUpdatedAt(LocalDateTime.now());
                DatabaseConnection.commitTransaction();
                return existing;
            } else {
                Friendship newBlock = new Friendship(blocker.getId(), userToBlock.getId(),
                        Friendship.FriendshipStatus.BLOCKED, blocker.getId());
                int friendshipId = friendshipDao.saveNewRequest(newBlock, conn);
                if (friendshipId <= 0)
                    throw new FriendshipOperationException(
                            "Failed to save new block record for user "
                                    + userToBlock.getUsername());
                newBlock.setFriendshipId(friendshipId);
                DatabaseConnection.commitTransaction();
                return newBlock;
            }
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (blockUser SQL): " + re.getMessage());}
            if (e instanceof DuplicateKeyException) {
                throw new FriendshipOperationException("A relationship record already exists that conflicts with blocking.", e);
            }
            throw new FriendshipOperationException("Database error blocking user.", e);
        } catch (FriendshipOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (blockUser Op/User): " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("FriendshipServiceImpl: Error ensuring transaction is rolled back in finally (blockUser): " + se.getMessage());}
            }
        }
    }

    @Override
    public void unblockUser(User unblocker, User userToUnblock)
            throws FriendshipOperationException, UserNotFoundException {
        if (unblocker == null || userToUnblock == null)
            throw new FriendshipOperationException("Unblocker and user to unblock cannot be null.");
        getUserOrThrow(unblocker.getId(), "unblocking user");
        getUserOrThrow(userToUnblock.getId(), "unblocking user (target)");
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Friendship friendship = getExistingFriendshipOrThrow(unblocker, userToUnblock,
                "No relationship record found with " + userToUnblock.getUsername()
                        + " to unblock.", conn);

            if (friendship.getStatus() != Friendship.FriendshipStatus.BLOCKED
                    || friendship.getActionUserId() != unblocker.getId()) {
                throw new FriendshipOperationException("You have not blocked "
                        + userToUnblock.getUsername() + " or the block was initiated by them.");
            }
            
            boolean success =
                    friendshipDao.deleteFriendship(unblocker.getId(), userToUnblock.getId(), conn);
            if (!success)
                throw new FriendshipOperationException(
                        "Failed to unblock user " + userToUnblock.getUsername());
            DatabaseConnection.commitTransaction();
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (unblockUser SQL): " + re.getMessage());}
            throw new FriendshipOperationException("Database error unblocking user.", e);
        } catch (FriendshipOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("FriendshipService: Error during rollback (unblockUser Op/User): " + re.getMessage());}
            throw e;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("FriendshipServiceImpl: Error ensuring transaction is rolled back in finally (unblockUser): " + se.getMessage());}
            }
        }
    }

    private List<User> getUsersFromFriendships(List<Friendship> friendships, int currentUserId)
            throws FriendshipOperationException {
        List<User> users = new ArrayList<>();
        for (Friendship f : friendships) {
            int otherUserId = f.getOtherUserId(currentUserId);
            try {
                userDao.findById(otherUserId).ifPresent(users::add);
            } catch (SQLException e) {
                throw new FriendshipOperationException(
                        "Database error fetching user details for friend list.", e);
            }
        }
        return users;
    }

    @Override
    public List<User> listFriends(User user)
            throws UserNotFoundException, FriendshipOperationException {
        getUserOrThrow(user.getId(), "listing friends");
        try {
            List<Friendship> acceptedFriendships = friendshipDao.findFriendshipsByUserIdAndStatus(
                    user.getId(), Friendship.FriendshipStatus.ACCEPTED);
            return getUsersFromFriendships(acceptedFriendships, user.getId());
        } catch (SQLException e) {
            throw new FriendshipOperationException("Database error listing friends.", e);
        }
    }

    @Override
    public List<User> listPendingIncomingRequests(User user)
            throws UserNotFoundException, FriendshipOperationException {
        getUserOrThrow(user.getId(), "listing incoming requests");
        try {
            List<Friendship> allPending = friendshipDao.findFriendshipsByUserIdAndStatus(
                    user.getId(), Friendship.FriendshipStatus.PENDING);
            List<Friendship> incoming =
                    allPending.stream().filter(f -> f.getActionUserId() != user.getId()).collect(Collectors.toList());
            return getUsersFromFriendships(incoming, user.getId());
        } catch (SQLException e) {
            throw new FriendshipOperationException(
                    "Database error listing incoming friend requests.", e);
        }
    }

    @Override
    public List<User> listPendingOutgoingRequests(User user)
            throws UserNotFoundException, FriendshipOperationException {
        getUserOrThrow(user.getId(), "listing outgoing requests");
        try {
            List<Friendship> allPending = friendshipDao.findFriendshipsByUserIdAndStatus(
                    user.getId(), Friendship.FriendshipStatus.PENDING);
            List<Friendship> outgoing =
                    allPending.stream().filter(f -> f.getActionUserId() == user.getId()).collect(Collectors.toList());
            return getUsersFromFriendships(outgoing, user.getId());
        } catch (SQLException e) {
            throw new FriendshipOperationException(
                    "Database error listing outgoing friend requests.", e);
        }
    }

    @Override
    public List<User> listBlockedUsers(User user)
            throws UserNotFoundException, FriendshipOperationException {
        getUserOrThrow(user.getId(), "listing blocked users");
        try {
            List<Friendship> allBlocked = friendshipDao.findFriendshipsByUserIdAndStatus(
                    user.getId(), Friendship.FriendshipStatus.BLOCKED);
            List<Friendship> usersBlockedByCurrentUser =
                    allBlocked.stream().filter(f -> f.getActionUserId() == user.getId()).collect(Collectors.toList());
            return getUsersFromFriendships(usersBlockedByCurrentUser, user.getId());
        } catch (SQLException e) {
            throw new FriendshipOperationException("Database error listing blocked users.", e);
        }
    }

    @Override
    public Friendship.FriendshipStatus getFriendshipStatus(User userA, User userB)
            throws UserNotFoundException, FriendshipOperationException {
        if (userA == null || userB == null)
            throw new FriendshipOperationException("Users cannot be null to check status.");
        getUserOrThrow(userA.getId(), "checking friendship status");
        getUserOrThrow(userB.getId(), "checking friendship status");

        if (userA.getId() == userB.getId())
            return Friendship.FriendshipStatus.ACCEPTED; 

        try {
            Optional<Friendship> friendshipOpt =
                    friendshipDao.findFriendship(userA.getId(), userB.getId());
            return friendshipOpt.map(Friendship::getStatus).orElse(null);
            
        } catch (SQLException e) {
            throw new FriendshipOperationException("Database error getting friendship status.", e);
        }
    }
}