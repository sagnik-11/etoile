package com.etoile.datesservice.security;

public enum Role {
    HIM,
    HER;

    public Role other() {
        return this == HIM ? HER : HIM;
    }
}
