package com.example.clsc.enums;

public enum ActionType {
    CREATE("create"),
    UPDATE("Update"),
    DELETE("Delete");

    private final String actionType;

    ActionType(String actionType){
        this.actionType = actionType;
    }
    public String getActionType(){return actionType;}
}
