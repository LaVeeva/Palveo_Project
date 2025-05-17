package com.palveo.model;

import java.util.Objects;

public class Tag {
    private int tagId;
    private String tagName; 

    public Tag() {
    }

    public Tag(String tagName) {
        this.tagName = tagName;
    }

    public Tag(int tagId, String tagName) {
        this.tagId = tagId;
        this.tagName = tagName;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        
        if (tagId > 0 && tag.tagId > 0) {
            return tagId == tag.tagId;
        }
        return Objects.equals(tagName != null ? tagName.toLowerCase() : null,
                              tag.tagName != null ? tag.tagName.toLowerCase() : null);
    }

    @Override
    public int hashCode() {
        
        if (tagId > 0) {
            return Objects.hash(tagId);
        }
        return Objects.hash(tagName != null ? tagName.toLowerCase() : null);
    }

    @Override
    public String toString() {
        return "Tag{" +
               "tagId=" + tagId +
               ", tagName='" + tagName + '\'' +
               '}';
    }
}