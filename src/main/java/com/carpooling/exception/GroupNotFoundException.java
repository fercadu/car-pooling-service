package com.carpooling.exception;

public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(int groupId) {
        super("Group " + groupId + " not found");
    }
}
