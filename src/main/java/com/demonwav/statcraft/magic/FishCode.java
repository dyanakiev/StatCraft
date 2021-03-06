/*
 * StatCraft Bukkit Plugin
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.statcraft.magic;

public enum FishCode {

    FISH    ((byte)0),
    TREASURE((byte)1),
    JUNK    ((byte)2);

    private byte code;

    FishCode(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static FishCode fromCode(byte code) {
        for (FishCode fishCode : values()) {
            if (code == fishCode.getCode())
                return fishCode;
        }
        return null;
    }
}
