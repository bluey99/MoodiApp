package com.example.asdproject.model;

public class Task {

    private String id;                 // Firestore document ID
    private String taskName;           // "Neighborhood Walk"
    private String parentId;           // id of the mom who created it
    private String childId;            // id of the child (Firestore doc id)
    private String displayWhen;        // "9/12/2025, 8:00PM"
    private String discussionPrompts;  // question text

    // Empty constructor required by Firestore
    public Task() { }

    public Task(String id,
                String taskName,
                String parentId,
                String childId,
                String displayWhen,
                String discussionPrompts,
                long createdAt) {
        this.id = id;
        this.taskName = taskName;
        this.parentId = parentId;
        this.childId = childId;
        this.displayWhen = displayWhen;
        this.discussionPrompts = discussionPrompts;
        //this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTaskName() { return taskName; }
    public String getParentId() { return parentId; }
    public String getChildId() { return childId; }
    public String getDisplayWhen() { return displayWhen; }
    public String getDiscussionPrompts() { return discussionPrompts; }
 //   public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setChildId(String childId) { this.childId = childId; }
    public void setDisplayWhen(String displayWhen) { this.displayWhen = displayWhen; }
    public void setDiscussionPrompts(String discussionPrompts) { this.discussionPrompts = discussionPrompts; }
  //  public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
