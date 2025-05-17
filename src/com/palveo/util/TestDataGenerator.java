package com.palveo.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Comment;
import com.palveo.model.Event;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.AuthService;
import com.palveo.service.CommentService;
import com.palveo.service.EventService;
import com.palveo.service.FriendshipService;
import com.palveo.service.ParticipantService;
import com.palveo.service.RatingService;
import com.palveo.service.TagService;
import com.palveo.service.UserService;
import com.palveo.service.impl.AuthServiceImpl;
import com.palveo.service.impl.CommentServiceImpl;
import com.palveo.service.impl.EventServiceImpl;
import com.palveo.service.impl.FriendshipServiceImpl;
import com.palveo.service.impl.ParticipantServiceImpl;
import com.palveo.service.impl.RatingServiceImpl;
import com.palveo.service.impl.TagServiceImpl;
import com.palveo.service.impl.UserServiceImpl;

public class TestDataGenerator {

    private static AuthService authService;
    private static UserService userService;
    private static EventService eventService;
    private static FriendshipService friendshipService;
    private static ParticipantService participantService;
    private static TagService tagService;
    private static RatingService ratingService;
    private static CommentService commentService;
    private static Random random = new Random();

    private static final String COMMON_PASSWORD = "Password123!";
    private static final String[] DEV_USERNAMES = {"aacamlibel", "bkucuk", "byildirim", "mesarigul", "omarici"};
    private static final String[] DEV_EMAILS = {"ahmet@example.com", "batuhank@example.com", "batuhany@example.com", "mehmet@example.com", "omer@example.com"};
    private static final String[] DEV_FIRSTNAMES = {"Ahmet Alp", "Batuhan", "Batuhan", "Mehmet Eren", "Omur Meric"};
    private static final String[] DEV_LASTNAMES = {"Camlibel", "Kucuk", "Yildirim", "Sarigul", "Arici"};
    private static final String[] DEV_CITIES = {"Ankara", "Istanbul", "Izmir", "Ankara", "Bursa"};
    private static final String[] DEV_DISTRICTS = {"Cankaya", "Kadikoy", "Bornova", "Yenimahalle", "Nilufer"};
    private static final String[] DEV_BIOS = {
            "Backend enthusiast, Java developer. Loves hiking and coding.",
            "Frontend wizard, focusing on user experience with JavaFX. Gamer.",
            "Full-stack developer, passionate about clean code and good food.",
            "UI/UX designer and FXML expert. Enjoys music and social events.",
            "Database architect and data integrity guardian. Loves concerts."
    };
    private static final String[] DEV_AVATARS = {
            "/images/avatars/default_avatar_m1.png", "/images/avatars/default_avatar_m2.png",
            "/images/avatars/default_avatar_f1.png", "/images/avatars/default_avatar_m3.png",
            "/images/avatars/default_avatar_f2.png"
    };
    private static final String DEFAULT_EVENT_IMAGE = "/images/default_event_image.png";
    private static final String RANDOM_USER_AVATAR = "/images/avatars/default_avatar_rnd.png";


    private static List<User> allUsers = new ArrayList<>();
    private static List<Event> createdEvents = new ArrayList<>();
    private static User randomUserAccount;

    public static void main(String[] args) {
        System.out.println("--- Palveo Test Data Generator ---");

        authService = new AuthServiceImpl();
        userService = new UserServiceImpl();
        eventService = new EventServiceImpl();
        friendshipService = new FriendshipServiceImpl();
        participantService = new ParticipantServiceImpl();
        tagService = new TagServiceImpl();
        ratingService = new RatingServiceImpl();
        commentService = new CommentServiceImpl();

        if (!DatabaseConnection.isConnected()) {
            System.err.println("[ERROR] Database not connected. Aborting.");
            return;
        }
        System.out.println("[INFO] DB connection OK. Starting Data Generation...");

        try {
            createDeveloperAccounts();
            createRandomUserAccount();
            if (allUsers.isEmpty()) { System.out.println("[WARN] No users created/found, further steps might be skipped or fail."); }

            enhanceProfiles();
            createFriendships();
            if (allUsers.size() < 2) { System.out.println("[WARN] Less than 2 users, friendship tests limited."); }

            createEvents();
            if (createdEvents.isEmpty()) { System.out.println("[WARN] No events created, further steps might be skipped or fail."); }

            if (!createdEvents.isEmpty()) {
                createEventParticipations();
                applyTagsToEventsAndUsers();
                createRatings();
                createEventAndProfileComments();
            } else {
                System.out.println("[INFO] Skipping event-dependent data generation as no events were created.");
            }
        } catch (Exception e) {
            System.err.println("[FATAL ERROR] Unexpected error during data generation:");
            e.printStackTrace();
        }

        System.out.println("\n[SUCCESS] Test Data Generation Process Completed.");
        System.out.println("===================================================");
        System.out.println("=== Account Credentials (Password for all is \"" + COMMON_PASSWORD + "\") ===");
        for (int i = 0; i < allUsers.size(); i++) {
            System.out.printf("%d. Username: %-20s Password: %s%n", (i + 1), allUsers.get(i).getUsername(), COMMON_PASSWORD);
        }
        System.out.println("===================================================");
        DatabaseConnection.closeConnection();
    }

    private static void createDeveloperAccounts() {
        System.out.println("\n[STEP 1/9] Creating Developer Accounts...");
        for (int i = 0; i < DEV_USERNAMES.length; i++) {
            String username = DEV_USERNAMES[i];
            try {
                Optional<User> existingUser = userService.getUserByUsername(username);
                if (existingUser.isPresent()) {
                    System.out.println("  [SKIP] User " + username + " already exists.");
                    if (allUsers.stream().noneMatch(u -> u.getId() == existingUser.get().getId())) {
                        allUsers.add(existingUser.get());
                    }
                    continue;
                }
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(DEV_EMAILS[i]);
                newUser.setFirstName(DEV_FIRSTNAMES[i]);
                newUser.setLastName(DEV_LASTNAMES[i]);
                newUser.setCity(DEV_CITIES[i]);
                newUser.setDistrict(DEV_DISTRICTS[i]);
                newUser.setEulaAccepted(true);
                newUser.setAgeVerified(true);
                User registeredUser = authService.registerUser(newUser, COMMON_PASSWORD, "What is your favorite childhood toy?", "Lego" + (i + 1));
                allUsers.add(registeredUser);
                System.out.println("  [OK] Registered: " + registeredUser.getUsername());
            } catch (Exception e) { System.err.println("  [ERROR] Registering " + username + ": " + e.getMessage()); }
        }
    }

    private static void createRandomUserAccount() {
        System.out.println("\n[STEP 2/9] Creating Random User Account...");
        String randomUsername = "randomUser" + UUID.randomUUID().toString().substring(0, 6);
        String randomEmail = randomUsername + "@example.com";
        try {
            Optional<User> existingUser = userService.getUserByUsername(randomUsername);
            if (existingUser.isPresent()) {
                System.out.println("  [SKIP] Random user " + randomUsername + " somehow already exists.");
                randomUserAccount = existingUser.get();
                if (allUsers.stream().noneMatch(u -> u.getId() == randomUserAccount.getId())) {
                    allUsers.add(randomUserAccount);
                }
                return;
            }
            User newUser = new User();
            newUser.setUsername(randomUsername);
            newUser.setEmail(randomEmail);
            newUser.setFirstName("Random");
            newUser.setLastName("Tester");
            newUser.setCity("Virtual City");
            newUser.setDistrict("Digital District");
            newUser.setEulaAccepted(true);
            newUser.setAgeVerified(true);
            newUser.setBio("Just a random user exploring Palveo!");
            newUser.setProfileImagePath(RANDOM_USER_AVATAR);
            randomUserAccount = authService.registerUser(newUser, COMMON_PASSWORD, "What is your lucky number?", Integer.toString(random.nextInt(100)));
            allUsers.add(randomUserAccount);
            System.out.println("  [OK] Registered Random User: " + randomUserAccount.getUsername());
        } catch (Exception e) { System.err.println("  [ERROR] Registering random user " + randomUsername + ": " + e.getMessage()); }
    }

    private static void enhanceProfiles() {
        System.out.println("\n[STEP 3/9] Enhancing User Profiles...");
        if (allUsers.isEmpty()) { System.out.println("  [SKIP] No users to enhance."); return; }

        List<User> usersToProcess = new ArrayList<>(allUsers);
        List<User> successfullyEnhancedUsers = new ArrayList<>();

        for (User userToEnhance : usersToProcess) {
            boolean isDev = false;
            int devIndex = -1;
            for(int i=0; i<DEV_USERNAMES.length; i++) {
                if(userToEnhance.getUsername().equals(DEV_USERNAMES[i])) {
                    isDev = true;
                    devIndex = i;
                    break;
                }
            }

            try {
                User updates = new User();
                updates.setId(userToEnhance.getId());
                if (isDev) {
                    updates.setBio(DEV_BIOS[devIndex % DEV_BIOS.length]);
                    updates.setProfileImagePath(DEV_AVATARS[devIndex % DEV_AVATARS.length]);
                } else if (randomUserAccount != null && userToEnhance.getId() == randomUserAccount.getId()) {
                    updates.setBio(userToEnhance.getBio());
                    updates.setProfileImagePath(userToEnhance.getProfileImagePath());
                } else {
                    updates.setBio("A Palveo User.");
                    updates.setProfileImagePath(RANDOM_USER_AVATAR);
                }

                userService.updateUserProfile(updates, userToEnhance);
                userService.getUserById(userToEnhance.getId()).ifPresent(successfullyEnhancedUsers::add);
                System.out.println("  [OK] Enhanced profile for: " + userToEnhance.getUsername());
            } catch (Exception e) {
                System.err.println("  [ERROR] Enhancing profile for " + userToEnhance.getUsername() + ": " + e.getMessage());
                successfullyEnhancedUsers.add(userToEnhance);
            }
        }
        allUsers = successfullyEnhancedUsers;
    }

    private static User getUserByUsername(String username) {
        return allUsers.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    private static void createFriendships() {
        System.out.println("\n[STEP 4/9] Creating Friendships...");
        if (allUsers.size() < 2) { System.out.println("  [SKIP] Not enough users for friendships."); return; }

        User u1 = getUserByUsername("aacamlibel");
        User u2 = getUserByUsername("bkucuk");
        User u3 = getUserByUsername("byildirim");
        User u4 = getUserByUsername("mesarigul");
        User u5 = getUserByUsername("omarici");
        User u6 = randomUserAccount;

        if (u1 == null || u2 == null || u3 == null || u4 == null || u5 == null || u6 == null) {
            System.err.println("  [WARN] One or more key users not found. Friendship creation might be limited.");
        }

        trySendRequestAndAccept(u1, u2);
        trySendRequestAndAccept(u3, u5);
        trySendRequestAndAccept(u1, u4);
        trySendRequestAndAccept(u2, u5);
        trySendRequestAndAccept(u2, u6);

        trySendRequest(u2, u3);
        trySendRequest(u5, u4);
        trySendRequest(u6, u1);
        trySendRequest(u4, u6);

        if (trySendRequest(u6, u3)) {
            try {
                friendshipService.rejectFriendRequest(u3, u6);
                System.out.println("  [OK] Declined: " + u6.getUsername() + " request by " + u3.getUsername());
            } catch (Exception e) {
                System.err.println("  [ERROR] Declining " + u6.getUsername() + " by " + u3.getUsername() + ": " + e.getMessage());
            }
        }

        if (u1 != null && u6 != null) {
            try {
                friendshipService.blockUser(u1, u6);
                System.out.println("  [OK] Blocked: " + u1.getUsername() + " blocked " + u6.getUsername());
            } catch (Exception e) {
                System.err.println("  [ERROR] Blocking " + u6.getUsername() + " by " + u1.getUsername() + ": " + e.getMessage());
            }
        }
    }

    private static boolean trySendRequest(User requester, User recipient) {
        if (requester == null || recipient == null) return false;
        try {
            friendshipService.sendFriendRequest(requester, recipient);
            System.out.println("  [OK] Pending: " + requester.getUsername() + " -> " + recipient.getUsername());
            return true;
        } catch (Exception e) {
            System.err.println("  [WARN] Sending req " + requester.getUsername() + "->" + recipient.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    private static void trySendRequestAndAccept(User requester, User recipient) {
        if (requester == null || recipient == null) return;
        if (trySendRequest(requester, recipient)) {
            try {
                friendshipService.acceptFriendRequest(recipient, requester);
                System.out.println("  [OK] Friends: " + requester.getUsername() + " <-> " + recipient.getUsername());
            } catch (Exception e) {
                System.err.println("  [WARN] Accepting friends " + requester.getUsername() + "<->" + recipient.getUsername() + ": " + e.getMessage());
            }
        }
    }

    private static class EventBuilder {
        private Event event = new Event();
        EventBuilder(String title, String description, LocalDateTime dateTime, String location, String category, Event.PrivacySetting privacy, String imagePath) {
            event.setTitle(title);
            event.setDescription(description);
            event.setEventDateTime(dateTime);
            event.setLocationString(location);
            event.setCategory(category);
            event.setPrivacy(privacy.toString());
            event.setEventImagePath(imagePath);
        }
        Event buildWithHost(User host) {
            if (host != null) {
                this.event.setHostUserId(host.getId());
            } else {
                System.err.println("[EventBuilder WARN] Host user is null for event: " + event.getTitle() + ". Host ID will not be set.");
            }
            return event;
        }
    }

    private static void createEvents() {
        System.out.println("\n[STEP 5/9] Creating Events...");
        createdEvents.clear();
        if (allUsers.isEmpty()) { System.out.println("  [SKIP] No users to host events."); return; }
        User ahmet = getUserByUsername("aacamlibel");
        User batuhanK = getUserByUsername("bkucuk");
        User batuhanY = getUserByUsername("byildirim");
        User mehmetE = getUserByUsername("mesarigul");
        User omur = getUserByUsername("omarici");
        User randomU = randomUserAccount;

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<Event> tempEventList = new ArrayList<>();

        try {
            if (ahmet != null) tempEventList.add(new EventBuilder("Java Study Group - Concurrency", "Deep dive into Java threads and executors.", now.plusDays(7).withHour(18).withMinute(0), "Bilkent Library Cafe", "Study", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(ahmet));
            if (batuhanK != null) tempEventList.add(new EventBuilder("Weekend Board Game Marathon", "Catan, Ticket to Ride, Gloomhaven! BYOS.", now.plusDays(10).withHour(15).withMinute(0), batuhanK.getCity() + " - " + batuhanK.getUsername() + "'s Den", "Gaming", Event.PrivacySetting.PRIVATE, DEFAULT_EVENT_IMAGE).buildWithHost(batuhanK));
            if (omur != null) tempEventList.add(new EventBuilder("Indie Music Showcase", "Discover new local bands.", now.plusDays(3).withHour(21).withMinute(0), "IF Performance Hall, Tunus", "Music", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(omur));
            if (ahmet != null) tempEventList.add(new EventBuilder("Tech Talk Tuesday: AI Ethics", "Discussion on AI implications.", now.plusDays(12).withHour(19).withMinute(0), "Online / Zoom", "Study", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(ahmet));
            if (randomU != null) tempEventList.add(new EventBuilder("Downtown Photography Walk", "Capture city scapes. All skill levels.", now.plusDays(5).withHour(14).withMinute(0), "Kızılay Square", "Social", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(randomU));
            if (mehmetE != null) tempEventList.add(new EventBuilder("Advanced Cooking Class: Pasta", "Learn to make fresh pasta. Limited spots!", now.plusDays(15).withHour(17).withMinute(0), "Culinary Institute", "Food", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(mehmetE));

            LocalDateTime soonEventBaseTime1 = now.plusSeconds(10);
            LocalDateTime soonEventBaseTime2 = now.plusSeconds(11);

            if (mehmetE != null) tempEventList.add(new EventBuilder("Eymir Lake Hike (Past)", "Refreshing hike, beautiful views.", soonEventBaseTime1, "Eymir Lake, Ankara", "Sports", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(mehmetE));
            if (batuhanY != null) tempEventList.add(new EventBuilder("Palveo Alpha Celebration (Past)", "Project milestone achieved! (Friends only)", soonEventBaseTime1.plusHours(2), "The Usual Spot", "Social", Event.PrivacySetting.PRIVATE, DEFAULT_EVENT_IMAGE).buildWithHost(batuhanY));
            if (batuhanK != null) tempEventList.add(new EventBuilder("Retro Game Night - NES Classics (Past)", "Duck Hunt, Mario, Zelda!", soonEventBaseTime2, batuhanK.getCity() + " - Batuhan K's Mancave", "Gaming", Event.PrivacySetting.PRIVATE, DEFAULT_EVENT_IMAGE).buildWithHost(batuhanK));
            if (omur != null) tempEventList.add(new EventBuilder("Book Club: Sci-Fi Classics (Past)", "Discussing 'Dune'.", soonEventBaseTime2.plusHours(1), "Online / Discord", "Study", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(omur));

            if (randomU != null) tempEventList.add(new EventBuilder("Empty Future Event", "This event will have no participants initially.", now.plusDays(20).withHour(10), "To be decided", "Other", Event.PrivacySetting.PUBLIC, DEFAULT_EVENT_IMAGE).buildWithHost(randomU));


            for (Event event : tempEventList) {
                User host = allUsers.stream().filter(u -> u.getId() == event.getHostUserId()).findFirst().orElse(null);
                if (host == null) {
                    System.err.println("  [ERROR] Host user not found for event '" + event.getTitle() + "'. Skipping event creation.");
                    continue;
                }
                try {
                    createdEvents.add(eventService.createEvent(event, host));
                    System.out.println("  [OK] Created Event: " + event.getTitle());
                } catch (Exception e) {
                    System.err.println("  [ERROR] Creating event '" + event.getTitle() + "': " + e.getMessage());
                }
            }
        } catch (Exception e) { System.err.println("  [ERROR] General error during event pre-creation: " + e.getMessage()); e.printStackTrace(); }
    }

    private static void createEventParticipations() {
        System.out.println("\n[STEP 6/9] Creating Event Participations...");
        if (allUsers.size() < 1 || createdEvents.isEmpty()) { System.out.println("  [SKIP] Not enough users or events."); return; }
        User ahmet = getUserByUsername("aacamlibel");
        User batuhanK = getUserByUsername("bkucuk");
        User batuhanY = getUserByUsername("byildirim");
        User mehmetE = getUserByUsername("mesarigul");
        User omur = getUserByUsername("omarici");
        User randomU = randomUserAccount;

        try {
            Event studyGroup = findEventByTitle("Java Study Group - Concurrency");
            Event boardGames = findEventByTitle("Weekend Board Game Marathon");
            Event eymirHikePast = findEventByTitle("Eymir Lake Hike (Past)");
            Event alphaPartyPast = findEventByTitle("Palveo Alpha Celebration (Past)");
            Event retroGameNightPast = findEventByTitle("Retro Game Night - NES Classics (Past)");

            if (studyGroup != null) {
                tryJoin(studyGroup, batuhanK);
                tryJoinAndThenDecline(studyGroup, mehmetE);
                tryJoin(studyGroup, omur);
                tryJoin(studyGroup, randomU);
            }
            if (boardGames != null) {
                tryJoin(boardGames, ahmet);
                tryJoin(boardGames, omur);
                tryJoin(boardGames, batuhanY);
                tryJoin(boardGames, randomU);
            }
             if (eymirHikePast != null && ahmet != null && mehmetE != null && omur != null) {
                tryJoin(eymirHikePast, omur);
                participantService.markAttendance(eymirHikePast.getId(), omur.getId(), mehmetE);
                tryJoin(eymirHikePast, ahmet);
                participantService.markAttendance(eymirHikePast.getId(), ahmet.getId(), mehmetE);
                tryJoin(eymirHikePast, randomU);
            }
            if (alphaPartyPast != null && batuhanY != null && omur != null && ahmet != null) {
                tryJoin(alphaPartyPast, omur);
                participantService.markAttendance(alphaPartyPast.getId(), omur.getId(), batuhanY);
                tryJoin(alphaPartyPast, ahmet);
            }
            if(retroGameNightPast != null && batuhanK != null && ahmet != null && omur != null && randomU != null) {
                tryJoin(retroGameNightPast, ahmet);
                participantService.markAttendance(retroGameNightPast.getId(), ahmet.getId(), batuhanK);
                tryJoin(retroGameNightPast, omur);
                participantService.markAttendance(retroGameNightPast.getId(), omur.getId(), batuhanK);
                tryJoin(retroGameNightPast, randomU);
            }

            System.out.println("  [OK] Event participations attempts completed (some may have been skipped due to privacy or other reasons).");
        } catch (Exception e) { System.err.println("  [ERROR] Creating participations: " + e.getMessage()); e.printStackTrace(); }
    }

    private static Event findEventByTitle(String title) {
        return createdEvents.stream().filter(e -> e.getTitle().equals(title)).findFirst().orElse(null);
    }

    private static void tryJoin(Event event, User user) {
        if (event == null || user == null) return;
        try {
            participantService.joinEvent(event.getId(), user);
            User host = getUserByUsername(allUsers.stream().filter(u->u.getId() == event.getHostUserId()).findFirst().get().getUsername());
            System.out.println("  [OK] Joined: " + user.getUsername() + " to " + (event.getPrivacy().equals(Event.PrivacySetting.PRIVATE.toString()) ? "private " : "") + "event '" + event.getTitle() + "'" + (host != null ? " (hosted by " + host.getUsername() + ")." : "."));
        } catch (Exception e) {
            System.out.println("  [INFO] Skipped join: " + user.getUsername() + " to event '" + event.getTitle() + "' (" + e.getMessage().split("\n")[0] + ").");
        }
    }
    private static void tryJoinAndThenDecline(Event event, User user) {
        if (event == null || user == null) return;
        try {
            participantService.joinEvent(event.getId(), user);
            participantService.updateRsvpStatus(event.getId(), user, Participant.RsvpStatus.DECLINED);
            System.out.println("  [OK] Joined then Declined: " + user.getUsername() + " for event '" + event.getTitle() + "'.");
        } catch (Exception e) {
            System.out.println("  [INFO] Skipped join/decline: " + user.getUsername() + " for event '" + event.getTitle() + "' (" + e.getMessage().split("\n")[0] + ").");
        }
    }


    private static void applyTagsToEventsAndUsers() {
        System.out.println("\n[STEP 7/9] Applying Tags to Events and Users...");
        if (allUsers.isEmpty()) { System.out.println("  [SKIP] No users available for tagging."); return; }
        User ahmet = getUserByUsername("aacamlibel");
        User batuhanK = getUserByUsername("bkucuk");
        User batuhanY = getUserByUsername("byildirim");
        User mehmetE = getUserByUsername("mesarigul");
        User omur = getUserByUsername("omarici");
        User randomU = randomUserAccount;

        try {
            tryApplyTagToUser(ahmet, "Backend", batuhanK);
            tryApplyTagToUser(ahmet, "Java Expert", omur);
            tryApplyTagToUser(ahmet, "Mentor", randomU);
            tryApplyTagToUser(batuhanK, "Frontend", ahmet);
            tryApplyTagToUser(batuhanK, "JavaFX", mehmetE);
            tryApplyTagToUser(batuhanK, "Gaming", randomU);
            tryApplyTagToUser(batuhanY, "FullStack", ahmet);
            tryApplyTagToUser(mehmetE, "UI_UX", batuhanK);
            tryApplyTagToUser(omur, "DB Guru", ahmet);
            tryApplyTagToUser(randomU, "Newbie", ahmet);
            System.out.println("  [OK] Various user tags applied attempts completed.");

            if (createdEvents.isEmpty()) { System.out.println("  [SKIP] No events to tag."); return; }
            Event studyGroup = findEventByTitle("Java Study Group - Concurrency");
            Event boardGames = findEventByTitle("Weekend Board Game Marathon");
            Event eymirHike = findEventByTitle("Eymir Lake Hike (Past)");
            Event indieMusic = findEventByTitle("Indie Music Showcase");
            Event techTalk = findEventByTitle("Tech Talk Tuesday: AI Ethics");
            Event photoWalk = findEventByTitle("Downtown Photography Walk");
            Event bookClub = findEventByTitle("Book Club: Sci-Fi Classics (Past)");

            tryApplyTagToEvent(studyGroup, "Education", ahmet);
            tryApplyTagToEvent(studyGroup, "Programming", ahmet);
            tryApplyTagToEvent(studyGroup, "Java", batuhanK);
            tryApplyTagToEvent(boardGames, "Social", batuhanK);
            tryApplyTagToEvent(boardGames, "Gaming", batuhanK);
            tryApplyTagToEvent(boardGames, "Fun", ahmet);
            tryApplyTagToEvent(eymirHike, "Outdoors", mehmetE);
            tryApplyTagToEvent(eymirHike, "Sports", mehmetE);
            tryApplyTagToEvent(indieMusic, "Live Music", omur);
            tryApplyTagToEvent(indieMusic, "Concert", omur);
            tryApplyTagToEvent(techTalk, "Tech", randomU);
            tryApplyTagToEvent(techTalk, "AI", ahmet);
            tryApplyTagToEvent(photoWalk, "Art", randomU);
            tryApplyTagToEvent(photoWalk, "Hobby", mehmetE);
            tryApplyTagToEvent(bookClub, "Literature", omur);
            tryApplyTagToEvent(bookClub, "Discussion", batuhanY);
            System.out.println("  [OK] Various event tags applied attempts completed.");

        } catch (Exception e) { System.err.println("  [ERROR] During tag application: " + e.getMessage()); e.printStackTrace();}
    }

    private static void tryApplyTagToUser(User targetUser, String tagName, User appliedByUser) {
        if (targetUser == null || appliedByUser == null || tagName == null || tagName.trim().isEmpty()) return;
        try {
            tagService.applyTagToUser(targetUser.getId(), tagName, appliedByUser);
        } catch (Exception e) {
            System.err.println("  [WARN] Applying tag '" + tagName + "' to user '" + targetUser.getUsername() + "' by '" + appliedByUser.getUsername() + "': " + e.getMessage());
        }
    }

    private static void tryApplyTagToEvent(Event event, String tagName, User appliedByUser) {
        if (event == null || appliedByUser == null || tagName == null || tagName.trim().isEmpty()) return;
        try {
            tagService.applyTagToEvent(event.getId(), tagName, appliedByUser);
        } catch (Exception e) {
            System.err.println("  [WARN] Applying tag '" + tagName + "' to event '" + event.getTitle() + "' by '" + appliedByUser.getUsername() + "': " + e.getMessage());
        }
    }

    private static void createRatings() {
        System.out.println("\n[STEP 8/9] Creating Ratings...");
        if (allUsers.size() < 2) { System.out.println("  [SKIP] Not enough users for ratings."); return; }
        User ahmet = getUserByUsername("aacamlibel");
        User batuhanK = getUserByUsername("bkucuk");
        User batuhanY = getUserByUsername("byildirim");
        User mehmetE = getUserByUsername("mesarigul");
        User omur = getUserByUsername("omarici");
        User randomU = randomUserAccount;

        try {
            tryRateUser(mehmetE, ahmet, 4, "Great eye for UI details!");
            tryRateUser(ahmet, batuhanK, 5, "Always helps with backend problems.");
            tryRateUser(omur, randomU, 3, "Seems knowledgeable about databases.");
            tryRateUser(batuhanK, mehmetE, 4, "Good presentation skills for JavaFX demos.");
            tryRateUser(batuhanY, ahmet, 5, "Solid full-stack understanding.");
            System.out.println("  [OK] User profile rating attempts completed.");

            if (createdEvents.isEmpty()) { System.out.println("  [SKIP] No events to rate."); return; }
            Event eymirHikePast = findEventByTitle("Eymir Lake Hike (Past)");
            Event alphaPartyPast = findEventByTitle("Palveo Alpha Celebration (Past)");
            Event retroGameNightPast = findEventByTitle("Retro Game Night - NES Classics (Past)");
            Event bookClubPast = findEventByTitle("Book Club: Sci-Fi Classics (Past)");

            tryRateEvent(eymirHikePast, omur, 5, "Eymir hike was fantastic!");
            tryRateEvent(eymirHikePast, ahmet, 4, "Good organization for the hike.");
            tryRateEvent(alphaPartyPast, omur, 5, "Alpha party was a blast!");
            tryRateEvent(retroGameNightPast, ahmet, 5, "Loved the retro games!");
            tryRateEvent(bookClubPast, randomU, 4, "Interesting book discussion.");
            System.out.println("  [OK] Event rating attempts completed (for past events).");

        } catch (Exception e) { System.err.println("  [ERROR] During rating creation: " + e.getMessage()); e.printStackTrace(); }
    }

    private static void tryRateUser(User ratedUser, User rater, int score, String comment) {
        if (ratedUser == null || rater == null) return;
        try {
            ratingService.submitRatingForUser(ratedUser.getId(), rater, score, comment);
        } catch (Exception e) {
            System.err.println("  [WARN] Rating user '" + ratedUser.getUsername() + "' by '" + rater.getUsername() + "': " + e.getMessage());
        }
    }
    private static void tryRateEvent(Event event, User rater, int score, String comment) {
        if (event == null || rater == null) return;
        try {
            ratingService.submitRatingForEvent(event.getId(), rater, score, comment);
        } catch (Exception e) {
            System.err.println("  [WARN] Rating event '" + event.getTitle() + "' by '" + rater.getUsername() + "': " + e.getMessage());
        }
    }

    private static void createEventAndProfileComments() {
        System.out.println("\n[STEP 9/9] Creating Event and Profile Comments...");
        if (allUsers.size() < 1) { System.out.println("  [SKIP] No users to create comments."); return; }
        User ahmet = getUserByUsername("aacamlibel");
        User batuhanK = getUserByUsername("bkucuk");
        User mehmetE = getUserByUsername("mesarigul");
        User omur = getUserByUsername("omarici");
        User randomU = randomUserAccount;

        try {
            if (ahmet != null && batuhanK != null) {
                Comment c1 = tryPostCommentToProfile(ahmet, batuhanK, "Your Java study group sounds interesting!");
                if (c1 != null) {
                    Comment c1_r1 = tryReplyToComment(c1, ahmet, "Thanks Batuhan! You should join us.");
                    if (randomU != null && c1_r1 != null) {
                        Comment c1_r1_r1 = tryReplyToComment(c1_r1, randomU, "I'm new to Java, is it beginner friendly?");
                         if(c1_r1_r1 != null) tryReplyToComment(c1_r1_r1, ahmet, "This one is advanced, but we can plan a beginner session!");
                    }
                }
            }
            if (randomU != null && mehmetE != null) tryPostCommentToProfile(randomU, mehmetE, "Welcome to Palveo, RandomUser!");
            if (omur != null && ahmet != null) tryPostCommentToProfile(omur, ahmet, "Great work on the database schema, Ahmet!");
            System.out.println("  [OK] Profile comment attempts completed.");

            if (createdEvents.isEmpty()) { System.out.println("  [SKIP] No events to comment on."); return; }
            Event studyGroup = findEventByTitle("Java Study Group - Concurrency");
            Event eymirHikePast = findEventByTitle("Eymir Lake Hike (Past)");
            Event boardGames = findEventByTitle("Weekend Board Game Marathon");
            Event techTalk = findEventByTitle("Tech Talk Tuesday: AI Ethics");

            if (studyGroup != null && mehmetE != null && ahmet != null) {
                Comment ec1 = tryPostCommentToEvent(studyGroup, mehmetE, "What version of Java will be covered?");
                if (ec1 != null) {
                    tryReplyToComment(ec1, ahmet, "We'll focus on Java 17+ features.");
                }
            }
            if (eymirHikePast != null && omur != null) tryPostCommentToEvent(eymirHikePast, omur, "The Eymir hike was so refreshing, thanks for organizing!");
            if (boardGames != null && randomU != null) tryPostCommentToEvent(boardGames, randomU, "Any recommendations for parking near the Board Game Den?");
            if (techTalk != null && batuhanK != null) tryPostCommentToEvent(techTalk, batuhanK, "Will the AI Ethics talk be recorded?");
            System.out.println("  [OK] Event comment attempts completed.");

        } catch (Exception e) { System.err.println("  [ERROR] During comment creation: " + e.getMessage()); e.printStackTrace();}
    }

    private static Comment tryPostCommentToProfile(User targetUser, User author, String content) {
        if (targetUser == null || author == null) return null;
        try {
            return commentService.postCommentToUserProfile(targetUser.getId(), author, content);
        } catch (Exception e) {
            System.err.println("  [WARN] Posting profile comment from '" + author.getUsername() + "' to '" + targetUser.getUsername() + "': " + e.getMessage());
            return null;
        }
    }
    private static Comment tryPostCommentToEvent(Event event, User author, String content) {
        if (event == null || author == null) return null;
        try {
            return commentService.postCommentToEvent(event.getId(), author, content);
        } catch (Exception e) {
            System.err.println("  [WARN] Posting event comment from '" + author.getUsername() + "' to event '" + event.getTitle() + "': " + e.getMessage());
            return null;
        }
    }
    private static Comment tryReplyToComment(Comment parentComment, User author, String content) {
        if (parentComment == null || author == null) return null;
        try {
            return commentService.replyToComment(parentComment.getCommentId(), author, content);
        } catch (Exception e) {
            System.err.println("  [WARN] Replying to comment ID " + parentComment.getCommentId() + " by '" + author.getUsername() + "': " + e.getMessage());
            return null;
        }
    }
}