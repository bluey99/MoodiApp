package com.example.asdproject.model;

public class Task {

    private String id;                 // Firestore document ID
    private String taskName;           // "Neighborhood Walk"

    private String childId;            // "507293184"

    private String creatorId;          // "parent2" or therapist id
    private String creatorType;        // "PARENT" or "THERAPIST"

    private String displayWhen;        // "31/1/2026, 10:29AM"
    private String discussionPrompts;  // "How are u?"

    private String status;             // "ASSIGNED" or "COMPLETED"

    // Empty constructor required by Firestore
    public Task() { }

    public String getId() { return id; }
    public String getTaskName() { return taskName; }
    public String getChildId() { return childId; }

    public String getCreatorId() { return creatorId; }
    public String getCreatorType() { return creatorType; }

    public String getDisplayWhen() { return displayWhen; }
    public String getDiscussionPrompts() { return discussionPrompts; }

    public String getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public void setChildId(String childId) { this.childId = childId; }

    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public void setCreatorType(String creatorType) { this.creatorType = creatorType; }

    public void setDisplayWhen(String displayWhen) { this.displayWhen = displayWhen; }
    public void setDiscussionPrompts(String discussionPrompts) { this.discussionPrompts = discussionPrompts; }

    public void setStatus(String status) { this.status = status; }
}
