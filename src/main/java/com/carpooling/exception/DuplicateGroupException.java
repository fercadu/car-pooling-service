package com.carpooling.exception;

public class DuplicateGroupException extends RuntimeException {

    public DuplicateGroupException(int groupId) {
        super("Group " + groupId + " already exists");
    }
}
