package server.manhunt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Manhunt extends JavaPlugin {

    private final Set<UUID> runners = new HashSet<>();
    private final Set<UUID> hunters = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("runner") && args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                runners.clear();
                runners.add(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + target.getName() + "님이 도망자로 설정되었습니다.");
            } else {
                sender.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("hunter") && args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                hunters.add(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + target.getName() + "님이 추격자로 설정되었습니다.");
            } else {
                sender.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("role") && sender instanceof Player) {
            Player player = (Player) sender;
            if (runners.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "당신은 도망자입니다.");
            } else if (hunters.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "당신은 추격자입니다.");
            } else {
                player.sendMessage(ChatColor.GRAY + "당신은 아무 역할도 없습니다.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("reset")) {
            runners.clear();
            hunters.clear();
            Bukkit.broadcastMessage(ChatColor.GRAY + "[게임] 역할이 초기화되었습니다.");
            return true;
        }

        if (label.equalsIgnoreCase("start")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("콘솔에서는 사용할 수 없습니다.");
                return true;
            }

            if (runners.size() != 1 || hunters.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "도망자는 정확히 한 명, 추격자는 최소 한 명 있어야 시작할 수 있습니다.");
                return true;
            }

            UUID runnerId = runners.iterator().next();
            Player runner = Bukkit.getPlayer(runnerId);

            if (runner == null) {
                sender.sendMessage(ChatColor.RED + "도망자 플레이어를 찾을 수 없습니다.");
                return true;
            }

            runner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1));
            runner.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 30, 1));
            runner.sendMessage(ChatColor.YELLOW + "30초간 도망칠 시간이 주어졌습니다!");
            runner.sendTitle(ChatColor.AQUA + "도망가세요!",null,10,40,10);

            for (UUID hunterId : hunters) {
                Player hunter = Bukkit.getPlayer(hunterId);
                if (hunter != null) {
                    hunter.teleport(runner.getLocation());
                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 30, 1));
                    hunter.setWalkSpeed(0f);
                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 30, 128));
                    hunter.sendMessage(ChatColor.RED + "30초간 대기하세요!");
                    hunter.sendTitle(ChatColor.AQUA + "기다리세요!",null,10,40,10);

                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        hunter.setWalkSpeed(0.2f);
                        hunter.sendMessage(ChatColor.GREEN + "추격을 시작하세요!");
                    }, 20 * 30L);
                }
            }

            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(ChatColor.GOLD + "시작!", "", 10, 40, 10);
                }
            }, 20 * 30L);

            Bukkit.broadcastMessage(ChatColor.GOLD + "게임이 시작되었습니다!");
            return true;
        }

        return false;
    }

    public boolean isRunner(Player player) {
        return runners.contains(player.getUniqueId());
    }

    public boolean isHunter(Player player) {
        return hunters.contains(player.getUniqueId());
    }

    public Set<UUID> getRunners() {
        return runners;
    }
}

class GameListener implements Listener {

    private final Manhunt plugin;
    private final Map<UUID, Long> diamondCooldown = new HashMap<>();

    public GameListener(Manhunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            Player killer = event.getEntity().getKiller();
            if (killer != null && plugin.isRunner(killer)) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "[게임 종료] 도망자가 엔더드래곤을 처치했습니다! 도망자의 승리!");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(ChatColor.GOLD + "도망자 승리!", "", 10, 40, 10); // 10초 동안 표시
                }
            } else {
                Bukkit.broadcastMessage(ChatColor.RED + "[게임 종료] 엔더드래곤이 처치되었지만 도망자가 아닙니다.");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(ChatColor.GOLD + "추격자 승리!", "", 10, 40, 10); // 10초 동안 표시
                }
            }
        }
    }

    @EventHandler
    public void onRunnerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.isRunner(player)) {
            Bukkit.broadcastMessage(ChatColor.RED + "[게임 종료] 도망자가 사망했습니다! 추격자의 승리!");
            player.sendTitle(ChatColor.GOLD + "추격자 승리!", "", 10, 40, 10);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // 우클릭 시 다이아몬드 확인
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.DIAMOND) return;

        // 쿨타임 체크
        long now = System.currentTimeMillis();
        long lastUsed = diamondCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - lastUsed < 30_000) {
            long remaining = (30_000 - (now - lastUsed)) / 1000;
            player.sendMessage("쿨타임: " + remaining + "초 남음");
            return;  // 쿨타임 중이면 다이아 소모를 하지 않음
        }

        // 쿨타임이 없으면 다이아 1개 소모
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getAmount() > 1) {
            itemInHand.setAmount(itemInHand.getAmount() - 1); // 다이아 1개 빼기
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); // 다이아가 1개일 때 공백 처리
        }
        if (plugin.getRunners().size() != 1) {
            player.sendMessage(ChatColor.RED + "도망자가 설정되지 않았습니다!");
            return;
        }

        Player runner = Bukkit.getPlayer(plugin.getRunners().iterator().next());
        if (runner == null) {
            player.sendMessage(ChatColor.RED + "도망자를 찾을 수 없습니다.");
            return;
        }

        Location loc = runner.getLocation();
        String worldName;
        if (loc.getWorld() != null) {
            switch (loc.getWorld().getName()) {
                case "world":
                    worldName = "오버월드";
                    break;
                case "world_nether":
                    worldName = "지옥";
                    break;
                case "world_the_end":
                    worldName = "엔드";
                    break;
                default:
                    worldName = loc.getWorld().getName(); // 다른 커스텀 월드의 경우 그대로 표시
            }
        } else {
            worldName = "알 수 없음";
        }
        String coords = String.format("도망자 좌표: [%s] X: %.1f Y: %.1f Z: %.1f", worldName, loc.getX(), loc.getY(), loc.getZ());
        player.sendMessage(ChatColor.AQUA + "[다이아] " + ChatColor.YELLOW + coords);
    }
}
