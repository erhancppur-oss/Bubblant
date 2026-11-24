package com.bubblant.game.engine;

import android.content.res.Resources;

import com.bubblant.game.R;

import java.util.Random;

/** Localized lines the ant can say. */
public final class Phrases {
    public final String[] left, right, up, down, wrong, angry, good, careful, ouch, thanks, drown,
            idle, air, wind, scared, friendJoin, needFriends, hurry, start, whirl, eel, fish, almost;
    private final Random rnd = new Random();

    public Phrases(Resources r) {
        left = r.getStringArray(R.array.say_left);
        right = r.getStringArray(R.array.say_right);
        up = r.getStringArray(R.array.say_up);
        down = r.getStringArray(R.array.say_down);
        wrong = r.getStringArray(R.array.say_wrong);
        angry = r.getStringArray(R.array.say_angry);
        good = r.getStringArray(R.array.say_good);
        careful = r.getStringArray(R.array.say_careful);
        ouch = r.getStringArray(R.array.say_ouch);
        thanks = r.getStringArray(R.array.say_thanks);
        drown = r.getStringArray(R.array.say_drown);
        idle = r.getStringArray(R.array.say_idle);
        air = r.getStringArray(R.array.say_air);
        wind = r.getStringArray(R.array.say_wind);
        scared = r.getStringArray(R.array.say_scared);
        friendJoin = r.getStringArray(R.array.say_friend);
        needFriends = r.getStringArray(R.array.say_need_friends);
        hurry = r.getStringArray(R.array.say_hurry);
        start = r.getStringArray(R.array.say_start);
        whirl = r.getStringArray(R.array.say_whirl);
        eel = r.getStringArray(R.array.say_eel);
        fish = r.getStringArray(R.array.say_fish);
        almost = r.getStringArray(R.array.say_almost);
    }

    public String pick(String[] a) {
        return a.length == 0 ? "" : a[rnd.nextInt(a.length)];
    }
}
