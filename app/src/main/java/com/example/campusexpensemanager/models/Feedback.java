package com.example.campusexpensemanager.models;

/**
 * Feedback Model - Priority 4
 * Represents user feedback/rating for the app
 */
public class Feedback {
    private int id;
    private int userId;
    private int rating; // 1-5 stars
    private String content;
    private long timestamp;

    // Constructor for new feedback
    public Feedback(int userId, int rating, String content, long timestamp) {
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Constructor for existing feedback (from DB)
    public Feedback(int id, int userId, int rating, String content, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.rating = rating;
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id=" + id +
                ", userId=" + userId +
                ", rating=" + rating +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}