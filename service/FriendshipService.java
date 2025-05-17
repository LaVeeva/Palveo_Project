package com.palveo.service;

import java.util.List;
import com.palveo.model.Friendship;
import com.palveo.model.User;
import com.palveo.service.exception.FriendshipOperationException;
import com.palveo.service.exception.UserNotFoundException;

public interface FriendshipService {
    Friendship sendFriendRequest(User requester, User recipient)
            throws FriendshipOperationException, UserNotFoundException;

    Friendship acceptFriendRequest(User acceptor, User requester)
            throws FriendshipOperationException, UserNotFoundException;

    Friendship rejectFriendRequest(User rejector, User requester)
            throws FriendshipOperationException, UserNotFoundException;

    void removeFriend(User remover, User friendToRemove)
            throws FriendshipOperationException, UserNotFoundException;

    Friendship blockUser(User blocker, User userToBlock)
            throws FriendshipOperationException, UserNotFoundException;

    void unblockUser(User unblocker, User userToUnblock)
            throws FriendshipOperationException, UserNotFoundException;

    List<User> listFriends(User user) throws UserNotFoundException, FriendshipOperationException;

    List<User> listPendingIncomingRequests(User user)
            throws UserNotFoundException, FriendshipOperationException;

    List<User> listPendingOutgoingRequests(User user)
            throws UserNotFoundException, FriendshipOperationException;

    List<User> listBlockedUsers(User user)
            throws UserNotFoundException, FriendshipOperationException; 

    Friendship.FriendshipStatus getFriendshipStatus(User userA, User userB)
            throws UserNotFoundException, FriendshipOperationException;
}
