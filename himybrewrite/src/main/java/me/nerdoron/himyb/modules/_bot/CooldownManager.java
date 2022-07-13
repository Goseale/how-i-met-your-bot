package me.nerdoron.himyb.modules._bot;

import me.nerdoron.himyb.Global;
import me.nerdoron.himyb.modules.Database;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class CooldownManager {
    private final Map<String, OffsetDateTime> COOLDOWNS = new HashMap<>();
    private static final Connection con = Database.connect();
    static PreparedStatement ps = null;

    public CooldownManager() {}

    public static String commandID(SlashCommandInteractionEvent event) {
        return "@"+event.getUser().getId()+event.getName()+"@";
    }

    public void addCooldown(String identifier, int timeInSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime plus = now.plusSeconds(timeInSeconds);
        COOLDOWNS.putIfAbsent(identifier, plus);
    }

    public void addCooldown(String identifier, String tag, int timeInSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime plus = now.plusSeconds(timeInSeconds);
        if (!COOLDOWNS.containsKey(identifier)) {
            COOLDOWNS.putIfAbsent(identifier+" #"+tag, plus);
        }
    }

    public boolean hasCooldown(String identifier) {
        OffsetDateTime now = OffsetDateTime.now();
        for (String cooldownKey : COOLDOWNS.keySet()) {
            if (cooldownKey.contains(identifier)) {
                OffsetDateTime cooldown = COOLDOWNS.get(cooldownKey);
                if (cooldown.isAfter(now)) {
                    return true;
                } else {
                    COOLDOWNS.remove(cooldownKey);
                    return false;
                }
            }
        }
        return false;
    }

    public boolean hasTag(String identifier, String tag) {
        for (String cooldownKey : COOLDOWNS.keySet()) {
            if (cooldownKey.contains(identifier)) {
                return cooldownKey.contains("#"+tag);
            }
        }
        return false;
    }

    public String parseCooldown(String identifier) {
        for (String cooldownKey : this.COOLDOWNS.keySet()) {
            if (cooldownKey.contains(identifier)) {
                return parseOffsetDateTimeHumanText(this.COOLDOWNS.get(cooldownKey));
            }
        }
        return "";
    }

    private String parseOffsetDateTimeHumanText(OffsetDateTime timeCreated) {
        OffsetDateTime now= OffsetDateTime.now();

        long sec = ChronoUnit.SECONDS.between(timeCreated,now)%60;
        long min = ChronoUnit.MINUTES.between(timeCreated,now)%60;
        long Hur = ChronoUnit.HOURS.between(timeCreated,now)%24;
        long day = ChronoUnit.DAYS.between(timeCreated,now)%31;
        long mth = ChronoUnit.MONTHS.between(timeCreated,now)%12;
        long yhr = ChronoUnit.YEARS.between(timeCreated,now);



        String send = "";

        if (yhr != 0) {
            send+= ""+Math.abs(yhr)+" Year";
        }
        if (mth != 0) {
            send+= ", "+Math.abs(mth)+" Month";
        }
        if (day != 0) {
            send+= ", "+Math.abs(day)+" Day";
        }
        if (Hur != 0) {
            send+= " "+Math.abs(Hur)+" hours";
        }
        if (min != 0) {
            send+= " "+Math.abs(min)+" minutes";
        }
        if (sec != 0) {
            send+= " "+Math.abs(sec)+" seconds";
        }
        return send.trim();
    }

    private Map<String, OffsetDateTime> DB_findIdentifier(String identifier) throws SQLException {
        String SQL = "SELECT * FROM cooldowns WHERE uid LIKE \""+identifier+"%\"";
        ps = con.prepareStatement(SQL);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Map<String, OffsetDateTime> r = new HashMap<>();
            String dbID = rs.getString(1);
            String dbOffsetText = rs.getString(2);
            r.put(dbID, parseTimestringToOffset(dbOffsetText));
            ps.close();
            return r;
            //Found
        } else {
            ps.close();
            //Not found;
        }
        return null;
    }

    private void DB_addNewEntry(String identifier, OffsetDateTime cooldownTime) throws SQLException {
        if (DB_findIdentifier(identifier) == null) {
            String statement = "INSERT INTO cooldowns (uid, cooldown) values(?,?)";
            PreparedStatement ps = con.prepareStatement(statement);
            ps.setString(1, identifier);
            ps.setString(2, parseOffsetToText(cooldownTime));
            ps.execute();
            ps.close();
        }
    }

    private void DB_removeEntry(String identifier) throws SQLException {
        Map<String, OffsetDateTime> cooldown = DB_findIdentifier(identifier);
        if (cooldown != null) {
            String statement = "DELETE FROM birthday WHERE uid = ?";
            PreparedStatement ps = con.prepareStatement(statement);
            ps.setString(1, Global.getNthElement(cooldown.keySet(),0));
            ps.execute();
        }
    }

    private static OffsetDateTime parseTimestringToOffset(String timestamp) {
        java.time.format.DateTimeFormatter parser = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        java.time.LocalDateTime dt = java.time.LocalDateTime.parse(timestamp, parser);
        ZonedDateTime zdt = ZonedDateTime.of(dt, java.time.ZoneId.of("UTC"));
        return OffsetDateTime.from(zdt);
    }

    private static String parseOffsetToText(OffsetDateTime offset) {
        return offset.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
}
