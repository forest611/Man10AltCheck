package red.man10.man10altcheck

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Man10AltCheck : JavaPlugin(), Listener {
    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()

        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    fun setAddress(p: Player) {

        val address = p.address.hostString

        if (address == null) {
            Bukkit.getLogger().info("IPAddressの取得に失敗！")
            return
        }

        val mysql = MySQLManager(this, "Man10AltCheck")

        val rs = mysql.query("SELECT * FROM user_list WHERE uuid='${p.uniqueId}';") ?: return

        if (rs.next()) {

            mysql.execute("UPDATE user_list SET address = '$address' player = '${p.name}' WHERE uuid = '${rs.getString("address")}';")

            rs.close()
            mysql.close()

            return
        }

        rs.close()
        mysql.close()

        mysql.execute("INSERT INTO user_list (player, uuid, address) VALUES ('${p.name}', '${p.uniqueId}', '${address}'); ")

    }

    fun getPlayers(name: String): MutableList<OfflinePlayer> {

        val list = mutableListOf<OfflinePlayer>()

        val mysql = MySQLManager(this, "Man10AltCheck")

        val p = Bukkit.getPlayer(name)

        val sql = if (p != null) "SELECT uuid FROM user_list WHERE address='${p.address.hostString}';"
        else "SELECT uuid FROM user_list WHERE player='$name';"

        val rs = mysql.query(sql) ?: return list

        while (rs.next()) {

            list.add(Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("uuid"))))

        }

        rs.close()
        mysql.close()

        return list
    }

    fun Listban(name: String): MutableList<OfflinePlayer>{
        val list = mutableListOf<OfflinePlayer>()

        val mysql = MySQLManager(this, "Man10AltCheck")

        val p = Bukkit.getPlayer(name)

        val sql = if (p != null) "SELECT uuid FROM user_list WHERE address='${p.address.hostString}';"
        else "SELECT uuid FROM user_list WHERE player='$name';"

        val rs = mysql.query(sql) ?: return list

        while (rs.next()) {

            mysql.execute("UPDATE player_data SET " +
                    "ban_until = DATE_ADD(ban_until,INTERVAL 0 MINUTE )," +
                    "ban_until = DATE_ADD(ban_until,INTERVAL 0 HOUR )," +
                    "ban_until = DATE_ADD(ban_until,INTERVAL 2100000000 DAY )" +
                    " where mcid='$name';")

        }
        return list
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (command.equals("altcheck")) {

            if (!sender.hasPermission("man10altcheck.check")) {
                return true
            }

            if (args.isEmpty()) {
                sender.sendMessage("§c§l/altcheck [username]")
                return true
            }

            GlobalScope.launch {

                val list = getPlayers(args[0])

                sender.sendMessage("§c§l現在検索中....")

                if (list.isEmpty()) {
                    sender.sendMessage("§c§l存在しないユーザー、もしくはIDが違う可能性があります")
                    return@launch
                }

                list.forEach { sender.sendMessage("§c§l${it.name}") }
            }
            return true
        }

        if (command.equals("listban")){
            if (!sender.hasPermission("man10altcheck.listban")) {
                return true
            }

            if (args.isEmpty()) {
                sender.sendMessage("§c§l/listban [username]")
                return true
            }

            GlobalScope.launch {

                val list = getPlayers(args[0])

                if (list.isEmpty()) {
                    sender.sendMessage("§c§l存在しないユーザー、もしくはIDが違う可能性があります")
                    return@launch
                }

                list.forEach { it.name?.let { it1 -> Listban(it1) } }
            }
        }

        return true
    }

    @EventHandler
    fun loginEvent(e: PlayerJoinEvent) {

        GlobalScope.launch {
            setAddress(e.player)
        }

    }
}