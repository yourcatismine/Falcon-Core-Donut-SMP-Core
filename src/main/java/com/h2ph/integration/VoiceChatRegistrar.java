package com.h2ph.integration;

import com.h2ph.Falcon;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.Bukkit;

public class VoiceChatRegistrar {
    
    public static void register(Falcon plugin) {
        BukkitVoicechatService service = Bukkit.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new VoiceChatIntegration(plugin));
        }
    }
}
