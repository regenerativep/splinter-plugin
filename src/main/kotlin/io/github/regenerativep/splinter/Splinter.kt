package io.github.regenerativep.splinter

import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
// import org.bukkit.event.EventHandler
// import org.bukkit.event.Listener
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class Splinter : JavaPlugin() {
    var serverId: Long = 0
    var playerUpdateTask: BukkitTask? = null
    override fun onEnable() {
        val config = this.config
        config.options().copyDefaults(true)
        this.saveConfig()
        this.serverId = config.getLong("serverId")

        var _this = this;
        this.server.messenger.registerIncomingPluginChannel(
                this,
                "splinter:splinter",
                PluginMessageListener({ channel: String, player: Player, data: ByteArray ->
                    _this.logger.info("got message on channel $channel")
                    var buf = ByteBuffer.wrap(data)
                    try {
                        val packet_id = buf.get()
                        when(packet_id.toInt()) {
                            1 -> packet_handle_position_set(buf, player)
                            else -> _this.logger.info("invalid packet id $packet_id")
                        }
                    }
                    catch(e: BufferUnderflowException) {
                        _this.logger.info("failed to read packet")
                    }
                })
        )
        this.server.messenger.registerOutgoingPluginChannel(this, "splinter:splinter")
        this.playerUpdateTask = (object : BukkitRunnable() {
            override fun run() {
                _this.server.onlinePlayers.forEach({ player ->
                    val loc = player.location
                    player.sendPluginMessage(
                        _this,
                        "splinter:splinter",
                        ByteBuffer.allocate(1+8+8+8)
                            .put(0.toByte())
                            .putDouble(loc.x)
                            .putDouble(loc.y)
                            .putDouble(loc.z)
                            .array(),
                    )
                })
            }
        }).runTaskTimer(this, 0, 20)
        // this.server.pluginManager.registerEvents(
        //         object : Listener {
        //             @EventHandler
        //             fun onPlayerJoin(event: PlayerJoinEvent) {
        //                 var channels = event.player.listeningPluginChannels
        //                 channels.add("splinter")

        //             }
        //         },
        //         this
        // )
    }
    override fun onDisable() {
        this.playerUpdateTask?.cancel()
        val config = this.config
        config.set("serverId", this.serverId)
        this.saveConfig()
    }
}

fun packet_handle_position_set(buf: ByteBuffer, player: Player) {
    val x = buf.getDouble();
    val y = buf.getDouble();
    val z = buf.getDouble();
    player.teleport(Location(player.world, x, y, z))
}
