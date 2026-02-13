package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HazardManager {

    private final PrismSurvival plugin;
    private final Map<UUID, PlayerActivity> activityStore = new ConcurrentHashMap<>();

    // Configurable thresholds
    private static final long SPEED_THRESHOLD_MS = 1500;
    private static final int REPETITION_THRESHOLD = 5;

    public HazardManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkActivity(UUID uuid, String type, String details) {
        PlayerActivity activity = activityStore.computeIfAbsent(uuid, k -> new PlayerActivity());
        long now = System.currentTimeMillis();

        // 1. Speed Check
        if (now - activity.lastActionTime < SPEED_THRESHOLD_MS) {
            logHazard(uuid, "SPEED", "Rapid interaction: " + details + " (" + (now - activity.lastActionTime) + "ms)");
        }

        // 2. Repetition Check
        if (details.equals(activity.lastActionDetails)) {
            activity.repetitionCount++;
            if (activity.repetitionCount >= REPETITION_THRESHOLD) {
                logHazard(uuid, "REPETITION", "Repetitive action (" + activity.repetitionCount + "x): " + details);
            }
        } else {
            activity.repetitionCount = 1;
        }

        activity.lastActionTime = now;
        activity.lastActionDetails = details;
    }

    private void logHazard(UUID uuid, String type, String details) {
        plugin.getActivityLogger().log(uuid, ActivityLogger.LogType.HAZARD, "[" + type + "] " + details);

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            String query = "INSERT INTO hazards (uuid, type, details, timestamp) VALUES (?, ?, ?, ?)";
            try (java.sql.Connection conn = plugin.getActivityLogger().getConnection();
                    PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type);
                ps.setString(3, details);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public int getHazardCount(UUID uuid) {
        String query = "SELECT COUNT(*) FROM hazards WHERE uuid = ?";
        try (java.sql.Connection conn = plugin.getActivityLogger().getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Map<String, Object>> getHazards(UUID uuid) {
        List<Map<String, Object>> hazards = new ArrayList<>();
        String query = "SELECT type, details, timestamp FROM hazards WHERE uuid = ? ORDER BY timestamp DESC";
        try (java.sql.Connection conn = plugin.getActivityLogger().getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> h = new HashMap<>();
                    h.put("type", rs.getString("type"));
                    h.put("details", rs.getString("details"));
                    h.put("timestamp", rs.getLong("timestamp"));
                    hazards.add(h);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hazards;
    }

    public int getTotalHazardousPlayersCount() {
        String query = "SELECT COUNT(DISTINCT uuid) FROM hazards";
        try (java.sql.Connection conn = plugin.getActivityLogger().getConnection();
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void resolveHazards(UUID uuid) {
        String query = "DELETE FROM hazards WHERE uuid = ?";
        try (java.sql.Connection conn = plugin.getActivityLogger().getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class PlayerActivity {
        long lastActionTime = 0;
        String lastActionDetails = "";
        int repetitionCount = 0;
    }
}
