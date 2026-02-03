package com.project.code_judge.Entity;

public enum Difficulty {
    EASY(0),
    MEDIUM(1),
    HARD(2);

    private final Integer level;
    Difficulty(int level){
        this.level = level;
    }

    public Integer getLevel() {
        return level;
    }
}
