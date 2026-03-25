import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Timer;

public class Vanguards extends JFrame {

    private static final Color PANEL_BG = new Color(28, 28, 38);
    private static final Color TEXT_LIGHT = new Color(245, 245, 245);
    private static final Color XP_ORANGE = new Color(255, 170, 0);
    private static final Color ENERGY_BLUE = new Color(0, 180, 255);
    private static final Color SHIELD_CYAN = new Color(100, 255, 210);
    private static final Font UI_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Serif", Font.BOLD, 120);
    private static Color ACCENT_COL = new Color(212, 175, 55);
    private static final Color[] TERRAIN_COLORS = {
            new Color(15, 15, 20), new Color(35, 15, 15), new Color(15, 30, 20),
            new Color(10, 20, 40), new Color(35, 25, 10), new Color(25, 10, 35), new Color(20, 30, 35)
    };
    private static final String[] WEAPON_PREFIXES = {"Flaming", "Freezing", "Venomous", "Divine", "Cursed", "Gilded", "Rusty", "Phantom", "Astral", "Savage", "Demonic", "Celestial", "Void", "Bloodthirst", "Ethereal"};
    private static final String[] WEAPON_NOUNS = {"Greatsword", "Staff", "Dagger", "Halberd", "Scythe", "Longbow", "Warhammer", "Katana", "Grimoire", "Whip"};
    private static final String[] ARMOR_PREFIXES = {"Sturdy", "Ethereal", "Spiked", "Heavy", "Lightweight", "Ancient", "Mythril", "Obsidian", "Silk", "Dragonbone", "Titanium", "Shadow", "Crystal"};
    private static final String[] ARMOR_NOUNS = {"Plate", "Robes", "Vest", "Armor", "Mantle", "Cuirass", "Cloak", "Tunic", "Carapace"};
    private static final String[] RELIC_NAMES = {"Sunblade", "Ogre Gauntlets", "Giant Belt", "Cloak of Shadows", "Amulet of Vitality", "Archmagi Robes", "Vorpal Shard", "Archer's Bracers", "Ring of Aegis", "Power Staff", "Eye of the Void", "Demon Horn", "Angel Wing"};
    private static final String[] MYTHIC_PASSIVES = {"Vampirism", "Regeneration", "Titan Shield", "Thorns"};
    private static final String[] GODLY_PASSIVES = {"Omnipotence", "Immortality Core", "Time Weaver", "Soul Syphon"};
    private static final String[] WEAPON_PASSIVES = {"Sorcerer's Echo", "Operator's Precision", "Knight's Resolve", "Shatter", "Swift Strike", "Lifesteal"};

    private GameState state;
    private CardLayout cards;
    private JPanel mainContainer;
    private BattlePanel battlePanel;
    private DeathScreen deathScreen;
    private IntroScreen introScreen;
    private BossWarningScreen bossWarningScreen;
    private List<Particle> particles = new ArrayList<>();
    private int screenShake = 0;
    private int maxComboAchieved = 0;
    private int rerollCost = 50;

    public Vanguards() {
        setTitle("Vanguards: World's Greatest Heroes");
        setSize(1350, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        for(int i = 0; i < 75; i++) particles.add(new Particle(Color.WHITE));

        state = new GameState();
        cards = new CardLayout();
        mainContainer = new JPanel(cards);

        mainContainer.add(new TitleScreen(), "TITLE");
        mainContainer.add(new TutorialScreen(), "TUTORIAL");
        mainContainer.add(new ClassSelect(), "SELECT");

        introScreen = new IntroScreen();
        mainContainer.add(introScreen, "INTRO");

        bossWarningScreen = new BossWarningScreen();
        mainContainer.add(bossWarningScreen, "BOSS_WARNING");

        battlePanel = new BattlePanel();
        mainContainer.add(battlePanel, "BATTLE");

        deathScreen = new DeathScreen();
        mainContainer.add(deathScreen, "DEATH");

        add(mainContainer);
        cards.show(mainContainer, "TITLE");
    }

    enum ClassType {
        KNIGHT("Knight", "Scales with: MAX HP & DEF", new Color(40, 70, 130), new Color(15, 25, 45)),
        SORCERER("Sorcerer", "Scales with: ATK & LUCK", new Color(100, 30, 150), new Color(30, 10, 45)),
        OPERATOR("Operator", "Scales with: ATK & SPD", new Color(160, 20, 30), new Color(40, 10, 15)),
        RANGER("Ranger", "Scales with: SPD & LUK", new Color(30, 120, 50), new Color(10, 30, 15)),
        PALADIN("Paladin", "Scales with: HP & ATK", new Color(200, 150, 20), new Color(45, 35, 10)),
        RONIN("Ronin", "Scales with: ATK & SPD", new Color(180, 50, 50), new Color(40, 15, 15));

        String title, scaleDesc; Color color, darkBg;
        ClassType(String t, String desc, Color c, Color bg) {
            title = t; scaleDesc = desc; color = c; darkBg = bg;
        }
    }

    enum BossArchetype {
        COLOSSUS("Colossus", new Color(200, 60, 60)),
        NECROMANCER("Necromancer", new Color(120, 40, 200)),
        MECHA_CORE("Mecha-Core", new Color(60, 200, 220)),
        VOID_DRAGON("Void Dragon", new Color(40, 10, 60));

        String name; Color color;
        BossArchetype(String n, Color c) { name = n; color = c; }
    }

    enum Rarity {
        COMMON(Color.LIGHT_GRAY, 1.0, "Common", 10),
        RARE(new Color(0, 150, 255), 1.8, "Rare", 25),
        EPIC(new Color(180, 50, 255), 3.0, "Epic", 75),
        LEGENDARY(new Color(255, 180, 0), 5.5, "Legendary", 200),
        MYTHIC(new Color(255, 50, 150), 9.0, "Mythical", 500),
        GODLY(new Color(0, 255, 200), 16.0, "Godly", 1500);

        Color col; double multiplier; String name; int sellValue;
        Rarity(Color c, double m, String n, int sv) {
            col = c; multiplier = m; name = n; sellValue = sv;
        }
    }

    enum EnemyType {
        BRUTE("Brute", new Color(200, 50, 50)), ASSASSIN("Assassin", new Color(50, 200, 100)),
        MAGE("Mage", new Color(150, 50, 200)), TANK("Tank", new Color(100, 100, 100));
        String typeName; Color typeColor;
        EnemyType(String n, Color c) { typeName = n; typeColor = c; }
    }

    enum EliteModifier {
        NONE(""), ARMORED("[Armored]"), VAMPIRIC("[Vampiric]"), SWIFT("[Swift]"), TOXIC("[Toxic]"), CORRUPTED("[Corrupted]");
        String tag; EliteModifier(String t) { tag = t; }
    }

    static class Item {
        String name; int price; Rarity rarity;
        public Item(String n, int p, Rarity r) { name = n; price = p; rarity = r; }
        public int getSellValue() { return Math.max(1, rarity.sellValue); }
    }

    abstract class Equipment extends Item {
        int hp, atk, def, spd, luk; String passive = null;
        public Equipment(String n, int p, int h, int a, int d, int s, int l, Rarity r) {
            super(n, p, r); hp = h; atk = a; def = d; spd = s; luk = l;
        }
        public String getStatsString() {
            return "HP: " + hp + " | ATK: " + atk + " | DEF: " + def + " | SPD: " + spd + " | LUK: " + luk;
        }
    }

    class Relic extends Equipment { public Relic(String n, int p, int h, int a, int d, int s, int l, Rarity r) { super(n, p, h, a, d, s, l, r); } }
    class Weapon extends Equipment { public Weapon(String n, int p, int h, int a, int d, int s, int l, Rarity r) { super(n, p, h, a, d, s, l, r); } }
    class Armor extends Equipment { public Armor(String n, int p, int h, int a, int d, int s, int l, Rarity r) { super(n, p, h, a, d, s, l, r); } }

    static class Consumable extends Item {
        String effect;
        public Consumable(String n, int p, String eff) { super(n, p, Rarity.COMMON); effect = eff; }
    }

    abstract class Entity {
        String name; int hp, maxHp, atk, def, spd, luk, level = 1; Color color;
        int shield = 0, burnTurns = 0, poisonTurns = 0, weakTurns = 0, vulnTurns = 0;
        int freezeTurns = 0, bleedTurns = 0;
        boolean stunned = false;
        double displayHp, displayEnergy = 0, displayXp = 0;
        int hitFlash = 0;
        int animX = 0, animY = 0, animTick = 0;
        String currentAnim = "NONE";
        Runnable onAnimComplete = null;

        public Entity(String n, int h, int a, int d, int s, int l, Color c) {
            name = n; maxHp = h; hp = h; atk = a; def = d; spd = s; luk = l;
            color = c; displayHp = hp;
        }

        public void takeDamage(int dmg) {
            if(vulnTurns > 0) dmg = (int)(dmg * 1.5);
            if(shield > 0) {
                if(dmg <= shield) { shield -= dmg; dmg = 0; }
                else { dmg -= shield; shield = 0; }
            }
            hp = Math.max(0, hp - dmg); hitFlash = 10;
        }
        public void heal(int amt) { hp = Math.min(maxHp, hp + amt); }
        public abstract void render(Graphics2D g, int x, int y, int tick);
        public void updateLiveStats() {
            displayHp += (hp - displayHp) * 0.15;
            if(hitFlash > 0) hitFlash--;
        }

        public void updateAnimation(int direction) {
            if(currentAnim.equals("NONE")) return;
            animTick++;
            if(currentAnim.equals("WINDUP")) {
                animX = (int)(-10 * direction * (animTick / 15.0));
                if(animTick >= 15) { currentAnim = "STRIKE"; animTick = 0; }
            } else if(currentAnim.equals("STRIKE")) {
                animX = (int)(60 * direction);
                if(animTick == 2 && onAnimComplete != null) { onAnimComplete.run(); onAnimComplete = null; }
                if(animTick >= 8) { currentAnim = "RETURN"; animTick = 0; }
            } else if(currentAnim.equals("RETURN")) {
                animX = (int)(60 * direction * (1.0 - (animTick / 10.0)));
                if(animTick >= 10) { currentAnim = "NONE"; animX = 0; animTick = 0; }
            }
        }
    }

    class Player extends Entity {
        ClassType cls;
        int xp = 0, energy = 50, maxEnergy = 100, gold = 50, unspentStats = 0, combo = 0;
        int healPots = 3, greaterPots = 1, dmgBuffs = 1, activeBuffTurns = 0, thornsTurns = 0;
        int winStreak = 0, fleePenalty = 10;
        int potionsUsedThisBattle = 0;
        boolean hasPet = false;

        List<Weapon> equippedWeapons = new ArrayList<>();
        List<Armor> equippedArmors = new ArrayList<>();
        List<Relic> equippedRelics = new ArrayList<>();
        List<Item> inventory = new ArrayList<>();

        public Player(ClassType c) {
            super(c.title, 300, 25, 20, 10, 5, c.color);
            this.cls = c;
            if(c == ClassType.KNIGHT) { maxHp = hp = 450; def = 35; }
            if(c == ClassType.SORCERER) { luk = 30; atk = 40; maxHp = hp = 220; }
            if(c == ClassType.OPERATOR) { spd = 30; atk = 35; maxHp = hp = 260; }
            if(c == ClassType.RANGER) { spd = 40; luk = 20; maxHp = hp = 240; atk = 30; def = 15; }
            if(c == ClassType.PALADIN) { maxHp = hp = 350; def = 25; atk = 35; spd = 5; luk = 5; }
            if(c == ClassType.RONIN) { maxHp = hp = 250; atk = 45; spd = 35; luk = 15; def = 10; }
            displayEnergy = energy;
        }

        public int[] getBonusStats() {
            int bH = 0, bA = 0, bD = 0, bS = 0, bL = 0;
            for(Weapon w : equippedWeapons) { bH += w.hp; bA += w.atk; bD += w.def; bS += w.spd; bL += w.luk; }
            for(Armor a : equippedArmors) { bH += a.hp; bA += a.atk; bD += a.def; bS += a.spd; bL += a.luk; }
            for(Relic r : equippedRelics) { bH += r.hp; bA += r.atk; bD += r.def; bS += r.spd; bL += r.luk; }
            bH = Math.min(bH, 2000); bA = Math.min(bA, 500); bD = Math.min(bD, 500);
            bS = Math.min(bS, 200); bL = Math.min(bL, 200);
            return new int[]{bH, bA, bD, bS, bL};
        }

        public int getTotalMaxHp() { return maxHp + getBonusStats()[0]; }

        @Override public void heal(int amt) {
            int diff = (hp + amt) - getTotalMaxHp();
            if(diff > 0 && Math.random() < 0.5) shield += diff;
            hp = Math.min(getTotalMaxHp(), hp + amt);
        }

        public int getExpRequirement() { return 100 + (level * level * 50); }

        public double getParryChance() { return Math.min(0.25, (spd + getBonusStats()[3]) * 0.005); }

        public String getRankTitle() {
            if(level < 5) return "Novice";
            if(level < 10) return "Adept";
            if(level < 15) return "Expert";
            if(level < 20) return "Master";
            if(level < 25) return "Grandmaster";
            return "Vanguard";
        }

        public boolean checkLevelUp(JTextArea log) {
            int target = getExpRequirement();
            if(xp >= target) {
                xp -= target; level++; hp = getTotalMaxHp(); energy = maxEnergy; unspentStats += 3;
                log.append("\n[SYS] LEVEL UP! Reached Level " + level + "\n");
                return true;
            }
            return false;
        }

        @Override public void updateLiveStats() {
            super.updateLiveStats();
            displayEnergy += (energy - displayEnergy) * 0.15;
            displayXp += (xp - displayXp) * 0.15;
            if(combo > maxComboAchieved) maxComboAchieved = combo;
            updateAnimation(1);
        }

        public void render(Graphics2D g, int x, int y, int tick) {
            int px = x + animX;
            double bounce = Math.sin(tick * 0.1) * 6; int py = (int) (y + bounce) + animY;

            if(currentAnim.equals("WINDUP")) {
                g.setColor(new Color(cls.color.getRed(), cls.color.getGreen(), cls.color.getBlue(), 100));
                int auraSize = 100 + (animTick * 2);
                g.fillOval(px - auraSize/2 + 25, py - auraSize/2 + 40, auraSize, auraSize);
            }

            if(winStreak >= 3 || combo > 0) {
                int intensity = Math.min(10, winStreak + combo);
                g.setColor(new Color(cls.color.getRed(), cls.color.getGreen(), cls.color.getBlue(), 40 + intensity * 10));
                for(int i = 0; i < 5 + intensity; i++) {
                    int fx = px - 25 + (int)(Math.random() * 80);
                    int fy = py + 90 - (int)(Math.random() * 120);
                    g.fillOval(fx, fy, 10 + (int)(Math.random()*20), 10 + (int)(Math.random()*20));
                }
            }

            g.setColor(new Color(0,0,0,100));
            g.fillOval(px-15, y+85, 90, 20);

            g.setColor(hitFlash > 0 ? Color.WHITE : color);
            if(shield > 0) {
                g.setColor(new Color(100, 200, 255, 100));
                g.fillOval(px-20, py-10, 100, 110);
                g.setColor(hitFlash > 0 ? Color.WHITE : color);
            }

            // Aesthetic enhancements for classes
            if(cls == ClassType.KNIGHT) {
                g.setColor(Color.LIGHT_GRAY); g.fillRoundRect(px-10, py+10, 30, 80, 5, 5); // Cape
                g.setColor(hitFlash > 0 ? Color.WHITE : color);
                g.fillRoundRect(px, py, 60, 85, 15, 15);
                g.setColor(new Color(50, 40, 60)); g.fillOval(px-20, py+30, 40, 50); // Shield
            }
            else if(cls == ClassType.SORCERER) {
                g.fillPolygon(new int[]{px+30, px-15, px+75}, new int[]{py, py+85, py+85}, 3);
                g.setColor(new Color(0, 255, 255)); g.fillOval(px+58, py-25, 20, 20); // Orb 1
                g.fillOval(px+10, py-30, 15, 15); // Orb 2
            }
            else if(cls == ClassType.RANGER) {
                g.setColor(new Color(20, 50, 20)); g.fillPolygon(new int[]{px-10, px+30, px+70}, new int[]{py+90, py+10, py+90}, 3); // Cloak
                g.setColor(hitFlash > 0 ? Color.WHITE : color);
                g.fillPolygon(new int[]{px, px+20, px+40}, new int[]{py+80, py, py+80}, 3);
                g.setColor(Color.GREEN); g.fillOval(px+15, py+30, 10, 10);
            }
            else if(cls == ClassType.PALADIN) {
                g.setColor(new Color(255, 215, 0, 100)); g.fillOval(px-10, py-10, 70, 70); // Holy Aura
                g.setColor(hitFlash > 0 ? Color.WHITE : color);
                g.fillRect(px, py, 50, 85);
                g.setColor(Color.YELLOW); g.fillRect(px+20, py+10, 10, 50); g.fillRect(px+10, py+30, 30, 10);
            }
            else if(cls == ClassType.RONIN) {
                g.fillRoundRect(px+5, py+15, 40, 70, 5, 5);
                g.setColor(Color.DARK_GRAY); g.fillOval(px-5, py, 60, 20); // Hat
                g.setColor(Color.LIGHT_GRAY); g.fillRect(px+45, py+40, 40, 5); // Katana
            }
            else { // OPERATOR
                g.fillRoundRect(px+10, py, 40, 85, 20, 20);
                g.setColor(Color.CYAN); g.fillRect(px+15, py+20, 30, 8); // Visor
                g.setColor(Color.DARK_GRAY); g.fillArc(px+5, py-5, 50, 45, 0, 180);
            }

            if(hasPet) {
                g.setColor(Color.CYAN);
                int petX = px - 40 + (int)(Math.sin(tick*0.1)*10);
                int petY = py + 20 + (int)(Math.cos(tick*0.15)*10);
                g.fillOval(petX, petY, 15, 15);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("<" + getRankTitle() + ">", px - 10, py - 35);

            int textY = py - 15;
            g.setFont(new Font("Impact", Font.PLAIN, 14));
            if(activeBuffTurns > 0) { g.setColor(Color.CYAN); g.drawString("ATK UP (" + activeBuffTurns + ")", px, textY); textY -= 15; }
            if(thornsTurns > 0) { g.setColor(Color.PINK); g.drawString("THORNS (" + thornsTurns + ")", px, textY); textY -= 15; }
            if(combo > 1) { g.setColor(new Color(255, 150, 255)); g.drawString("COMBO x" + combo, px, textY); textY -= 15; }
        }
    }

    class Enemy extends Entity {
        boolean isBoss;
        int turnCounter = 0; EnemyType archetype; EliteModifier elite = EliteModifier.NONE;
        BossArchetype bossArchetype;

        public Enemy(String n, int stage, boolean boss) {
            super(boss ? "OVERLORD: " + n : n, 0,0,0,0,0, Color.WHITE);
            this.isBoss = boss; this.level = stage;

            if(boss) {
                BossArchetype[] bTypes = BossArchetype.values();
                this.bossArchetype = bTypes[new Random().nextInt(bTypes.length)];
                this.color = bossArchetype.color;
                this.name = "OVERLORD: " + bossArchetype.name;
            } else {
                EnemyType[] types = EnemyType.values();
                this.archetype = types[new Random().nextInt(types.length)];
                this.color = archetype.typeColor;
            }

            if(!boss && Math.random() < 0.3) {
                EliteModifier[] mods = {EliteModifier.ARMORED, EliteModifier.VAMPIRIC, EliteModifier.SWIFT, EliteModifier.TOXIC, EliteModifier.CORRUPTED};
                this.elite = mods[new Random().nextInt(mods.length)];
            }
            if(!boss) this.name = elite.tag + " " + archetype.typeName + " " + n;

            double scaleFactor = Math.pow(1.15, stage);
            if(stage > 10) scaleFactor *= Math.pow(1.05, stage - 10);

            // Increased Boss Base HP
            int baseHP = (int)((boss ? 700 : 90) * scaleFactor);
            int baseATK = (int)((boss ? 45 : 18) * scaleFactor);
            int baseDEF = (int)((boss ? 30 : 10) * scaleFactor);
            int baseSPD = (int)(10 + (stage * 2));

            if(!boss) {
                if(archetype == EnemyType.BRUTE) { baseHP *= 1.3; baseATK *= 1.2; baseDEF *= 0.8; }
                else if(archetype == EnemyType.ASSASSIN) { baseHP *= 0.8; baseATK *= 1.5; baseSPD *= 2; }
                else if(archetype == EnemyType.TANK) { baseHP *= 1.5; baseDEF *= 1.8; baseATK *= 0.7; }
                else if(archetype == EnemyType.MAGE) { baseATK *= 1.8; baseHP *= 0.7; }

                if(elite == EliteModifier.ARMORED) baseDEF *= 2.0;
                if(elite == EliteModifier.SWIFT) baseSPD *= 2.5;
                if(elite == EliteModifier.CORRUPTED) { baseHP *= 2.5; baseATK *= 1.8; this.color = Color.MAGENTA; }
            } else {
                if(bossArchetype == BossArchetype.COLOSSUS) { baseHP *= 1.5; baseDEF *= 1.5; baseSPD *= 0.5; }
                else if(bossArchetype == BossArchetype.NECROMANCER) { baseATK *= 1.5; baseHP *= 0.8; }
                else if(bossArchetype == BossArchetype.VOID_DRAGON) { baseSPD *= 2.0; baseATK *= 1.3; }
            }

            this.maxHp = this.hp = baseHP; this.atk = baseATK;
            this.def = baseDEF; this.spd = baseSPD;
            this.luk = 2; this.displayHp = this.hp;
        }

        @Override public void updateLiveStats() {
            super.updateLiveStats();
            updateAnimation(-1);
        }

        public void render(Graphics2D g, int x, int y, int tick) {
            int px = x + animX;
            double hover = Math.sin(tick * 0.08) * 10;
            int py = y + (isBoss ? -60 : 20) + (int)hover + animY;

            if(currentAnim.equals("WINDUP")) {
                g.setColor(new Color(255, 50, 50, 100));
                int auraSize = 100 + (animTick * 2);
                g.fillOval(px - auraSize/2 + 50, py - auraSize/2 + 50, auraSize, auraSize);
            }

            g.setColor(new Color(0,0,0,100)); g.fillOval(px-20, y+85, 110, 20);
            Color renderCol = hitFlash > 0 ? Color.WHITE : (burnTurns > 0 ? Color.ORANGE : (poisonTurns > 0 ? Color.GREEN : color));
            if(isBoss && hp < maxHp * 0.3) renderCol = hitFlash > 0 ? Color.WHITE : new Color(220, 40, 80);

            g.setColor(renderCol);

            if (isBoss) {
                if(bossArchetype == BossArchetype.COLOSSUS) {
                    g.fillRect(px, py, 140, 140);
                    g.setColor(Color.RED); g.fillOval(px+40, py+40, 20, 20); g.fillOval(px+80, py+40, 20, 20);
                } else if(bossArchetype == BossArchetype.NECROMANCER) {
                    g.fillPolygon(new int[]{px+70, px+140, px}, new int[]{py-40, py+140, py+140}, 3);
                    g.setColor(Color.MAGENTA); g.fillOval(px-20, py+(int)(Math.sin(tick*0.1)*10), 30, 30);
                    g.fillOval(px+130, py+(int)(Math.cos(tick*0.1)*10), 30, 30);
                } else if(bossArchetype == BossArchetype.MECHA_CORE) {
                    g.fillOval(px+10, py+10, 120, 120);
                    g.setColor(Color.CYAN);
                    g.drawArc(px-10, py-10, 160, 160, tick*5, 180);
                    g.drawArc(px, py, 140, 140, -tick*5, 180);
                    g.fillOval(px+50, py+50, 40, 40);
                } else if(bossArchetype == BossArchetype.VOID_DRAGON) {
                    int[] dx = {px+10, px+70, px+130, px+150, px+90, px-20};
                    int[] dy = {py+40, py-20, py+60, py+140, py+100, py+120};
                    g.fillPolygon(dx, dy, 6);
                    g.setColor(Color.YELLOW); g.fillOval(px+50, py+10, 15, 15);
                }
            } else {
                if(archetype == EnemyType.TANK) { g.fillRoundRect(px, py, 70, 70, 10, 10); }
                else if(archetype == EnemyType.ASSASSIN) { Polygon p = new Polygon(new int[]{px+35, px+70, px}, new int[]{py, py+70, py+70}, 3); g.fillPolygon(p); }
                else { Polygon minion = new Polygon(new int[]{px+35, px+70, px+35, px}, new int[]{py, py+35, py+70, py+35}, 4); g.fillPolygon(minion); }
                g.setColor(Color.BLACK); g.fillOval(px+20, py+20, 30, 30);
                g.setColor(Color.RED); g.fillOval(px+30, py+30, 10, 10);
            }

            int textY = py - 20;
            g.setFont(new Font("Impact", Font.PLAIN, 16));
            if(stunned) { g.setColor(Color.YELLOW); g.drawString("STUNNED", px, textY); textY -= 20; }
            if(burnTurns > 0) { g.setColor(Color.ORANGE); g.drawString("BURN (" + burnTurns + ")", px, textY); textY -= 20;}
            if(poisonTurns > 0) { g.setColor(Color.GREEN); g.drawString("POISON (" + poisonTurns + ")", px, textY); textY -= 20;}
            if(weakTurns > 0) { g.setColor(Color.LIGHT_GRAY); g.drawString("WEAK (" + weakTurns + ")", px, textY); textY -= 20;}
            if(vulnTurns > 0) { g.setColor(Color.MAGENTA); g.drawString("VULN (" + vulnTurns + ")", px, textY); textY -= 20;}
            if(isBoss && turnCounter > 8) { g.setColor(Color.RED); g.drawString("ENRAGED!", px, textY); }
        }
    }

    class Particle {
        double x, y, speed, size; Color c;
        public Particle(Color c) {
            this.c = c; speed = 0.5 + Math.random() * 2;
            x = Math.random() * 1350; y = Math.random() * 480; size = 2 + Math.random() * 4;
        }
        public void update() {
            y -= speed; if(y < 0) { y = 480; x = Math.random() * 1350; }
        }
        public void draw(Graphics2D g) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.fillOval((int)x, (int)y, (int)size, (int)size);
        }
    }

    class DamageNumber {
        int val, x, y, life=40; Color c; String text = null;
        public DamageNumber(int v, int x, int y, Color c) {
            val=v; this.x=x + (int)(Math.random() * 40 - 20); this.y=y + (int)(Math.random() * 20 - 10); this.c=c;
        }
        public DamageNumber(int v, int x, int y, Color c, String t) { this(v,x,y,c); text=t; }
        public void draw(Graphics2D g) {
            String display = text != null ? text : (c==Color.GREEN ? "+" : "-") + val;
            g.setFont(new Font("Impact", Font.PLAIN, 28));
            g.setColor(new Color(0,0,0, Math.max(0, life*5)));
            g.drawString(display, x-1, y-1); g.drawString(display, x+1, y-1);
            g.drawString(display, x-1, y+1); g.drawString(display, x+1, y+1);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, life*6)));
            g.drawString(display, x, y);
        }
    }

    class IntroScreen extends JPanel {
        private ClassType cls;
        private int tick = 0;
        private Timer animTimer;

        public IntroScreen() { setBackground(Color.BLACK); }

        public void play(ClassType ct, Runnable onComplete) {
            this.cls = ct; this.tick = 0;
            if(animTimer != null) animTimer.stop();
            animTimer = new Timer(30, e -> {
                tick++; repaint();
                if(tick > 80) { animTimer.stop(); onComplete.run(); }
            });
            animTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(cls == null) return;
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(cls.color.getRed(), cls.color.getGreen(), cls.color.getBlue(), Math.min(255, tick * 3)));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Serif", Font.BOLD, 80));
            FontMetrics fm = g2.getFontMetrics();
            String t = cls.title.toUpperCase();
            g2.drawString(t, (getWidth() - fm.stringWidth(t))/2, getHeight()/2);
        }
    }

    class BossWarningScreen extends JPanel {
        private int tick = 0;
        private Timer animTimer;

        public BossWarningScreen() { setBackground(Color.BLACK); }

        public void play(Runnable onComplete) {
            this.tick = 0;
            if(animTimer != null) animTimer.stop();
            animTimer = new Timer(30, e -> {
                tick++; repaint();
                if(tick > 90) { animTimer.stop(); onComplete.run(); }
            });
            animTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int fade = Math.min(255, tick * 5);
            g2.setColor(new Color(fade / 2, 0, 0));
            g2.fillRect(0, 0, getWidth(), getHeight());

            if(tick % 10 < 5) {
                g2.setColor(Color.RED);
                g2.setFont(new Font("Impact", Font.ITALIC, 100));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("WARNING: OVERLORD APPROACHES", (getWidth() - fm.stringWidth("WARNING: OVERLORD APPROACHES"))/2, getHeight()/2);
            }
        }
    }

    class TutorialScreen extends JPanel {
        public TutorialScreen() {
            setLayout(null);
            setBackground(PANEL_BG);

            JLabel title = new JLabel("HOW TO PLAY");
            title.setFont(new Font("Serif", Font.BOLD, 60));
            title.setForeground(ACCENT_COL);
            title.setBounds(470, 80, 500, 80);
            add(title);

            String tutorialHtml = "<html><body style='width: 800px; color: #F5F5F5; font-family: SansSerif; font-size: 20px; line-height: 1.8;'>"
                    + "<div style='text-align: center; margin-bottom: 20px;'><font color='#D4AF37'><b>COMBAT SYSTEM</b></font><br>Spend Energy to use class abilities. String attacks together to build Combos for massive damage multipliers.</div>"
                    + "<table style='width: 100%; font-size: 18px;'>"
                    + "<tr><td style='width: 50%; padding: 10px;'>"
                    + "<font color='#D4AF37'><b>PRIMARY STATS</b></font><br>"
                    + "• ATK: Increases damage dealt.<br>"
                    + "• DEF: Reduces damage taken.<br>"
                    + "• SPD: Increases Evasion & Parry chances.<br>"
                    + "• LUK: Increases Critical Hit chance."
                    + "</td><td style='width: 50%; padding: 10px;'>"
                    + "<font color='#D4AF37'><b>EQUIPMENT & LIMITS</b></font><br>"
                    + "• Weapons: Max 2 equipped<br>"
                    + "• Armor: Max 4 equipped<br>"
                    + "• Relics: Max 10 equipped<br>"
                    + "Equipment grants massive stat bonuses and passives."
                    + "</td></tr></table>"
                    + "<div style='text-align: center; margin-top: 20px;'><font color='#D4AF37'><b>SHOP & BOUNTIES</b></font><br>Slay enemies to earn Gold and complete Bounties. Use Gold in the Shop to buy gear, potions, and upgrades. The Shop refreshes every 3 battles.<br><br>"
                    + "<font color='#D4AF37'><b>MYSTERY BOX PITY</b></font><br>If you hit 4 low-tier drops in a row, the 5th box guarantees Epic or higher.</div>"
                    + "</body></html>";

            JLabel desc = new JLabel(tutorialHtml);
            desc.setBounds(275, 200, 800, 400);
            add(desc);

            StylizedButton btn = new StylizedButton("CHOOSE YOUR HERO");
            btn.setFont(new Font("SansSerif", Font.BOLD, 24));
            btn.setBounds(500, 650, 350, 60);
            btn.addActionListener(e -> cards.show(mainContainer, "SELECT"));
            add(btn);
        }
    }

    class BattlePanel extends JPanel {
        private Enemy enemy;
        private int tick = 0; public JTextArea log;
        private CardLayout menuCards = new CardLayout();
        private JPanel menuPanel = new JPanel(menuCards);
        private JPanel sideShopPanel = new JPanel(new BorderLayout());
        private Map<String, JPanel> subMenus = new HashMap<>();
        private List<DamageNumber> dmgNums = new ArrayList<>();
        private List<Item> shopStock = new ArrayList<>();
        private int encountersUntilRefresh = 3; private Timer gameLoop;
        private boolean autoSellCommon = false, autoSellRare = false, autoSellEpic = false;
        private boolean inputLocked = false;
        private int lastInventoryTab = 0;
        private String currentMenuKey = "MAIN";
        private int mysteryBoxPity = 0;

        public BattlePanel() {
            setLayout(new BorderLayout());
            setBackground(TERRAIN_COLORS[0]);
            JPanel renderArea = new JPanel() { protected void paintComponent(Graphics g) { super.paintComponent(g); drawScene((Graphics2D)g); } };
            renderArea.setPreferredSize(new Dimension(950, 480)); renderArea.setOpaque(false);
            JPanel bottomSection = new JPanel(new BorderLayout());
            bottomSection.setPreferredSize(new Dimension(950, 380)); bottomSection.setBackground(PANEL_BG);
            bottomSection.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(4, 0, 0, 0, ACCENT_COL),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            log = new JTextArea();
            log.setBackground(new Color(15, 15, 22)); log.setForeground(TEXT_LIGHT); log.setFont(new Font("Monospaced", Font.PLAIN, 14));
            log.setEditable(false); log.setMargin(new Insets(10, 10, 10, 10));
            JScrollPane logScroll = new JScrollPane(log);
            logScroll.setPreferredSize(new Dimension(450, 300));
            logScroll.setBorder(BorderFactory.createLineBorder(ACCENT_COL.darker(), 2));

            menuPanel.setOpaque(false);
            bottomSection.add(logScroll, BorderLayout.WEST); bottomSection.add(menuPanel, BorderLayout.CENTER);

            sideShopPanel.setPreferredSize(new Dimension(300, 900));
            sideShopPanel.setBackground(new Color(20, 20, 28));

            add(renderArea, BorderLayout.CENTER); add(bottomSection, BorderLayout.SOUTH);
            add(sideShopPanel, BorderLayout.EAST);

            gameLoop = new Timer(30, e -> {
                tick++; updateDmg(); if(screenShake > 0) screenShake--;
                for(Particle p : particles) p.update();
                if(state.player != null) state.player.updateLiveStats();
                if(enemy != null) enemy.updateLiveStats();
                log.setCaretPosition(log.getDocument().getLength());
                repaint();
            });
        }

        public void initSession() {
            state.encounters = 1;
            state.bountyKills = 0; state.bountyTarget = 3; state.bountyReward = 100;
            maxComboAchieved = 0; inputLocked = false; rerollCost = 50; mysteryBoxPity = 0;
            setupMenus(); spawnEnemy(); refreshShop();
            log.setText("[SYS] THE RIFT AWAKENS\n");
            sideShopPanel.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, state.player.cls.color));
            gameLoop.start();
        }

        private void setupMenus() {
            menuPanel.removeAll(); subMenus.clear();
            JPanel mainWrapper = new JPanel(new BorderLayout());
            mainWrapper.setOpaque(false);
            subMenus.put("MAIN", mainWrapper);
            menuPanel.add(mainWrapper, "MAIN");
            String[] pages = {"ATTACKS", "INVENTORY", "ITEMS", "STATS", "LEVEL_UP", "EQUIPMENT_DASH", "EMPTY"};
            for(String s : pages) {
                JPanel sub = new JPanel(new BorderLayout()); sub.setOpaque(false);
                subMenus.put(s, sub); menuPanel.add(sub, s);
            }
            setMenu("MAIN");
        }

        private JPanel createMainActions() {
            JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
            JPanel grid = new JPanel(new GridLayout(2, 4, 15, 15));
            grid.setBorder(new EmptyBorder(20, 30, 10, 30)); grid.setOpaque(false);
            String[] btns = {"ATTACKS", "DEFEND (+Shield)", "CAMP (10G)", "INVENTORY", "STATS", "FLEE (" + state.player.fleePenalty + "G)"};
            for(String s : btns) {
                StylizedButton b = new StylizedButton(s);
                if (s.startsWith("CAMP") && state.player.gold < 10) b.setForeground(Color.DARK_GRAY);
                else if (s.startsWith("FLEE")) {
                    if (state.player.gold < state.player.fleePenalty) b.setForeground(Color.DARK_GRAY); else b.setForeground(Color.MAGENTA);
                }
                b.addActionListener(e -> {
                    if(inputLocked) return;
                    if(state.player.unspentStats > 0) { setMenu("LEVEL_UP"); return; }
                    if(s.startsWith("DEFEND")) executeDefend();
                    else if(s.startsWith("CAMP")) executeRest();
                    else if(s.startsWith("FLEE")) executeFlee();
                    else setMenu(s.equals("INVENTORY") ? "EQUIPMENT_DASH" : s);
                });
                grid.add(b);
            }
            JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5)); quickBar.setOpaque(false);
            addConsumableBtn(quickBar, "Minor Potion", () -> state.player.healPots, e -> { state.player.heal(150); state.player.healPots--; log.append("[COMBAT] Healed 150 HP\n"); dmgNums.add(new DamageNumber(150, 280, 280, Color.GREEN)); endPlayerTurn(); });
            addConsumableBtn(quickBar, "Greater Potion", () -> state.player.greaterPots, e -> { state.player.heal(350); state.player.greaterPots--; log.append("[COMBAT] Healed 350 HP\n"); dmgNums.add(new DamageNumber(350, 280, 280, Color.GREEN)); endPlayerTurn(); });
            addConsumableBtn(quickBar, "Damage Buff", () -> state.player.dmgBuffs, e -> { state.player.activeBuffTurns += 3; state.player.dmgBuffs--; log.append("[COMBAT] DMG Buff applied for 3 turns!\n"); endPlayerTurn(); });
            p.add(grid, BorderLayout.CENTER); p.add(quickBar, BorderLayout.SOUTH);
            return p;
        }

        public void setMenu(String key) {
            currentMenuKey = key;
            if(key.equals("MAIN")) {
                JPanel mainP = subMenus.get("MAIN"); mainP.removeAll();
                mainP.add(createMainActions(), BorderLayout.CENTER); mainP.revalidate(); mainP.repaint();
            } else if (!key.equals("EMPTY")) { refreshSubMenu(key); }
            menuCards.show(menuPanel, key);
        }

        private void refreshSubMenu(String key) {
            JPanel p = subMenus.get(key);
            if(p == null) return; p.removeAll(); Player player = state.player;
            JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15)); content.setOpaque(false);

            if(key.equals("ATTACKS")) {
                List<String> moves = new ArrayList<>(); moves.add("Basic Strike (0 EN)");
                if(player.cls == ClassType.KNIGHT) {
                    moves.add("Shield Bash (15 EN)"); if(player.level >= 3) moves.add("Aegis Crush (35 EN)"); if(player.level >= 5) moves.add("Phalanx (40 EN) [THORNS]");
                }
                else if(player.cls == ClassType.SORCERER) {
                    moves.add("Fireball (15 EN) [BURN]"); if(player.level >= 3) moves.add("Void Storm (40 EN)"); if(player.level >= 5) moves.add("Curse of Weakness (30 EN) [WEAK]");
                }
                else if(player.cls == ClassType.OPERATOR) {
                    moves.add("Backstab (15 EN)"); if(player.level >= 3) moves.add("Execution (35 EN) [HEAL]"); if(player.level >= 5) moves.add("Toxic Dart (20 EN) [POISON]");
                }
                else if(player.cls == ClassType.RANGER) {
                    moves.add("Piercing Arrow (15 EN)"); if(player.level >= 3) moves.add("Volley (35 EN) [WEAK]"); if(player.level >= 5) moves.add("Snipe (40 EN)");
                }
                else if(player.cls == ClassType.PALADIN) {
                    moves.add("Holy Strike (15 EN)"); if(player.level >= 3) moves.add("Divine Favor (30 EN) [HEAL]"); if(player.level >= 5) moves.add("Smite (40 EN) [BURN]");
                }
                else if(player.cls == ClassType.RONIN) {
                    moves.add("Quick Draw (15 EN)"); if(player.level >= 3) moves.add("Wind Slash (35 EN)"); if(player.level >= 5) moves.add("Dragon Strike (40 EN) [BURN]");
                }

                for(String m : moves) {
                    StylizedButton b = new StylizedButton(m); b.setPreferredSize(new Dimension(240, 50));
                    int cost = m.contains("(15 EN)") ? 15 : m.contains("(20 EN)") ? 20 : m.contains("(30 EN)") ? 30 : m.contains("(35 EN)") ? 35 : m.contains("(40 EN)") ? 40 : 0;
                    if(player.energy < cost) b.setForeground(Color.DARK_GRAY);
                    b.addActionListener(e -> { if(!inputLocked) executeAttack(m); }); content.add(b);
                }
            } else if(key.equals("EQUIPMENT_DASH")) {
                JPanel dash = new JPanel(new BorderLayout(5, 5)); dash.setOpaque(false); dash.setPreferredSize(new Dimension(480, 230));
                JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); filters.setOpaque(false);
                JCheckBox chkCom = new JCheckBox("Auto-Sell Com"); chkCom.setForeground(Color.WHITE); chkCom.setOpaque(false); chkCom.setSelected(autoSellCommon); chkCom.addActionListener(e -> autoSellCommon = chkCom.isSelected());
                JCheckBox chkRar = new JCheckBox("Auto-Sell Rare"); chkRar.setForeground(Color.WHITE); chkRar.setOpaque(false); chkRar.setSelected(autoSellRare); chkRar.addActionListener(e -> autoSellRare = chkRar.isSelected());
                JCheckBox chkEpi = new JCheckBox("Auto-Sell Epic"); chkEpi.setForeground(Color.WHITE); chkEpi.setOpaque(false); chkEpi.setSelected(autoSellEpic); chkEpi.addActionListener(e -> autoSellEpic = chkEpi.isSelected());
                filters.add(chkCom); filters.add(chkRar); filters.add(chkEpi); dash.add(filters, BorderLayout.NORTH);

                JTabbedPane tabs = new JTabbedPane(); tabs.addChangeListener(e -> lastInventoryTab = tabs.getSelectedIndex());
                JPanel wPan = new JPanel(new GridLayout(0, 2, 10, 10)); wPan.setBackground(PANEL_BG);
                JPanel aPan = new JPanel(new GridLayout(0, 2, 10, 10)); aPan.setBackground(PANEL_BG);
                JPanel rPan = new JPanel(new GridLayout(0, 2, 10, 10)); rPan.setBackground(PANEL_BG);
                for(Item it : player.inventory) {
                    if(it instanceof Equipment) {
                        Equipment eq = (Equipment)it;
                        boolean isEquipped = player.equippedRelics.contains(eq) || player.equippedWeapons.contains(eq) || player.equippedArmors.contains(eq);
                        JPanel card = createItemCard(eq, isEquipped, player);
                        if(eq instanceof Weapon) wPan.add(card); else if(eq instanceof Armor) aPan.add(card); else if(eq instanceof Relic) rPan.add(card);
                    }
                }

                JPanel wWrap = new JPanel(new BorderLayout()); wWrap.setBackground(PANEL_BG); wWrap.add(wPan, BorderLayout.NORTH);
                JPanel aWrap = new JPanel(new BorderLayout()); aWrap.setBackground(PANEL_BG); aWrap.add(aPan, BorderLayout.NORTH);
                JPanel rWrap = new JPanel(new BorderLayout()); rWrap.setBackground(PANEL_BG); rWrap.add(rPan, BorderLayout.NORTH);

                JScrollPane wScroll = new JScrollPane(wWrap); wScroll.setPreferredSize(new Dimension(450, 160)); wScroll.setBorder(null); wScroll.getVerticalScrollBar().setUnitIncrement(16);
                JScrollPane aScroll = new JScrollPane(aWrap); aScroll.setPreferredSize(new Dimension(450, 160)); aScroll.setBorder(null); aScroll.getVerticalScrollBar().setUnitIncrement(16);
                JScrollPane rScroll = new JScrollPane(rWrap); rScroll.setPreferredSize(new Dimension(450, 160)); rScroll.setBorder(null); rScroll.getVerticalScrollBar().setUnitIncrement(16);

                tabs.addTab("Weapons (" + player.equippedWeapons.size() + "/2)", wScroll); tabs.addTab("Armor (" + player.equippedArmors.size() + "/4)", aScroll); tabs.addTab("Relics (" + player.equippedRelics.size() + "/10)", rScroll);
                if(lastInventoryTab < tabs.getTabCount()) tabs.setSelectedIndex(lastInventoryTab);
                dash.add(tabs, BorderLayout.CENTER); content.add(dash);

            } else if(key.equals("STATS")) {
                int[] b = player.getBonusStats();
                JPanel statBox = new JPanel(new GridLayout(6, 2, 10, 10)); statBox.setOpaque(false);
                statBox.add(createStatLabel("CLASS:", player.cls.title, ACCENT_COL)); statBox.add(createStatLabel("LEVEL:", player.level+"", Color.WHITE));
                statBox.add(createStatLabel("MAX HP:", player.maxHp + " (+" + b[0] + ")", Color.WHITE)); statBox.add(createStatLabel("ATK:", player.atk + " (+" + b[1] + ")", Color.WHITE));
                statBox.add(createStatLabel("DEF:", player.def + " (+" + b[2] + ")", Color.WHITE)); statBox.add(createStatLabel("SPD:", player.spd + " (+" + b[3] + ")", Color.WHITE));
                statBox.add(createStatLabel("LUK:", player.luk + " (+" + b[4] + ")", Color.WHITE));
                double dodge = Math.min(0.60, (player.spd + b[3]) * 0.012) * 100; statBox.add(createStatLabel("EVASION:", String.format("%.1f%%", dodge), Color.CYAN));
                double parry = player.getParryChance() * 100; statBox.add(createStatLabel("PARRY CHANCE:", String.format("%.1f%%", parry), Color.YELLOW));
                content.add(statBox);
            } else if(key.equals("LEVEL_UP")) {
                content.add(new JLabel("<html><font size='6' color='"+toHex(ACCENT_COL)+"'>POINTS: "+player.unspentStats+"</font></html>"));
                String[] sNames = {"ATTACK", "DEFENSE", "SPEED", "LUCK", "HP (+20)"};
                for(String s : sNames) {
                    StylizedButton b = new StylizedButton("+1 " + s);
                    b.addActionListener(e -> {
                        if(player.unspentStats > 0) {
                            if(s.equals("ATTACK")) player.atk++; else if(s.equals("DEFENSE")) player.def++; else if(s.equals("SPEED")) player.spd++; else if(s.equals("LUCK")) player.luk++; else { player.maxHp+=20; player.hp+=20; }
                            player.unspentStats--; if(player.unspentStats == 0) setMenu("MAIN"); else setMenu("LEVEL_UP");
                        }
                    });
                    content.add(b);
                }
            }
            p.add(content, BorderLayout.CENTER);
            if(!key.equals("LEVEL_UP") && !key.equals("EMPTY")) {
                JPanel bottomAnchor = new JPanel(new FlowLayout(FlowLayout.CENTER)); bottomAnchor.setOpaque(false);
                StylizedButton back = new StylizedButton("BACK"); back.addActionListener(e -> { setMenu("MAIN"); }); bottomAnchor.add(back); p.add(bottomAnchor, BorderLayout.SOUTH);
            }
            p.revalidate(); p.repaint();
        }

        private JPanel createItemCard(Equipment eq, boolean isEq, Player p) {
            JPanel c = new JPanel(new BorderLayout()); c.setBorder(BorderFactory.createLineBorder(eq.rarity.col, 2)); c.setBackground(new Color(25,25,30));
            JLabel n = new JLabel("<html>" + (isEq?"<b>[E]</b> ":"") + eq.name + "</html>"); n.setForeground(eq.rarity.col); n.setFont(new Font("SansSerif", Font.BOLD, 12)); n.setBorder(new EmptyBorder(5, 5, 0, 5));
            n.setToolTipText("<html>" + eq.getStatsString() + "<br>" + (eq.passive!=null ? "<b>" + eq.passive + "</b>" : "") + "</html>");
            JLabel s = new JLabel("<html>"+eq.getStatsString()+"<br>"+(eq.passive!=null?eq.passive:"")+"</html>"); s.setForeground(Color.LIGHT_GRAY); s.setFont(new Font("SansSerif", Font.PLAIN, 10)); s.setBorder(new EmptyBorder(5, 5, 5, 5));
            JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2)); bp.setOpaque(false);
            JButton eb = new JButton(isEq?"UNEQUIP":"EQUIP"); eb.setFont(new Font("SansSerif", Font.BOLD, 10));
            eb.addActionListener(e -> {
                if(isEq) {
                    if(eq instanceof Relic) p.equippedRelics.remove(eq); else if(eq instanceof Weapon) p.equippedWeapons.remove(eq); else if(eq instanceof Armor) p.equippedArmors.remove(eq);
                } else {
                    if(eq instanceof Relic) { if(p.equippedRelics.size() < 10) p.equippedRelics.add((Relic)eq); else log.append("Relics full (10/10)!\n"); }
                    else if(eq instanceof Weapon) { if(p.equippedWeapons.size() < 2) p.equippedWeapons.add((Weapon)eq); else log.append("Weapons full (2/2)!\n"); }
                    else if(eq instanceof Armor) { if(p.equippedArmors.size() < 4) p.equippedArmors.add((Armor)eq); else log.append("Armor full (4/4)!\n"); }
                }
                setMenu("EQUIPMENT_DASH");
            });
            JButton sb = new JButton("SELL(" + eq.rarity.sellValue + ")"); sb.setFont(new Font("SansSerif", Font.BOLD, 10)); if(isEq) sb.setEnabled(false);
            sb.addActionListener(e -> { p.inventory.remove(eq); p.gold += eq.rarity.sellValue; log.append("[SYS] Sold " + eq.name + "\n"); setMenu("EQUIPMENT_DASH"); buildSideShop(); });
            bp.add(eb); bp.add(sb); c.add(n, BorderLayout.NORTH); c.add(s, BorderLayout.CENTER); c.add(bp, BorderLayout.SOUTH); return c;
        }

        private void addConsumableBtn(JPanel p, String name, Supplier<Integer> countSupplier, ActionListener a) {
            StylizedButton b = new StylizedButton("<html><center>" + name + "<br>(" + countSupplier.get() + ")</center></html>"); b.setPreferredSize(new Dimension(140, 45)); b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.addActionListener(e -> { if(!inputLocked && countSupplier.get() > 0) a.actionPerformed(e); });
            if(countSupplier.get() == 0) b.setForeground(Color.DARK_GRAY); p.add(b);
        }

        private void buildSideShop() {
            sideShopPanel.removeAll(); Player p = state.player;
            JPanel header = new JPanel(new GridLayout(3,1)); header.setOpaque(false); header.setBorder(new EmptyBorder(20,20,10,20));
            header.add(new JLabel("<html><font size='5' color='"+toHex(ACCENT_COL)+"'>MERCHANT</font></html>"));
            header.add(new JLabel("<html><font size='4' color='white'>Gold: " + p.gold + "G</font></html>")); header.add(new JLabel("<html><font color='gray'>Refreshes in: " + encountersUntilRefresh + " battles</font></html>"));
            sideShopPanel.add(header, BorderLayout.NORTH);
            JPanel items = new JPanel(); items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS)); items.setOpaque(false); items.setBorder(new EmptyBorder(10, 10, 10, 10));

            for(Item it : shopStock) {
                StylizedButton btn = new StylizedButton("<html><center>" + it.name + "<br>" + it.price + "G</center></html>");
                btn.setPreferredSize(new Dimension(240, 60)); btn.setMaximumSize(new Dimension(240, 60)); btn.setAlignmentX(Component.CENTER_ALIGNMENT);
                if(it instanceof Equipment) { btn.setForeground(((Equipment)it).rarity.col); btn.setToolTipText("<html>" + ((Equipment)it).getStatsString() + "<br>" + (((Equipment)it).passive!=null ? "<b>" + ((Equipment)it).passive + "</b>" : "") + "</html>"); }
                else if(it.name.contains("Elixir")) btn.setForeground(Color.MAGENTA); else if(it.name.contains("Mystery Box")) btn.setForeground(Color.YELLOW); else if(it.name.contains("Familiar Crystal")) btn.setForeground(Color.CYAN);
                if(p.gold < it.price) btn.setForeground(Color.DARK_GRAY);
                btn.addActionListener(e -> {
                    if(p.gold >= it.price) {
                        p.gold -= it.price;
                        if(it instanceof Equipment) p.inventory.add(it);
                        else {
                            if(it.name.contains("Greater")) p.greaterPots++; else if(it.name.contains("Damage")) p.dmgBuffs++;
                            else if(it.name.contains("Mystery Box")) { Equipment dropped = generateProceduralEquipment(true); p.inventory.add(dropped); log.append("[GACHA] Box contained: " + dropped.name + " (" + dropped.rarity.name + ")!\n"); }
                            else if(it.name.contains("Elixir")) { p.atk += 2; p.def += 2; log.append("[SYS] Gained +2 Permanent ATK/DEF!\n"); }
                            else if(it.name.contains("Familiar Crystal")) { p.hasPet = true; log.append("[SYS] Summoned a Familiar!\n"); }
                            else if(it.name.contains("Forge Weapon")) {
                                if(!p.equippedWeapons.isEmpty()) { p.equippedWeapons.get(0).atk += 15; log.append("[SYS] Weapon Forged! +15 ATK to Primary Weapon\n"); } else { p.gold += it.price; log.append("[SYS] No weapon equipped to forge!\n"); shopStock.add(it); }
                            } else p.healPots++;
                        }
                        shopStock.remove(it); log.append("[SYS] Purchased " + it.name + "!\n"); buildSideShop(); if(currentMenuKey.equals("EQUIPMENT_DASH")) setMenu("EQUIPMENT_DASH");
                    } else { log.append("[SYS] Not enough Gold!\n"); }
                });
                items.add(btn); items.add(Box.createRigidArea(new Dimension(0, 10)));
            }

            StylizedButton reroll = new StylizedButton("Reroll Shop (" + rerollCost + "G)");
            reroll.setPreferredSize(new Dimension(240, 40)); reroll.setMaximumSize(new Dimension(240, 40)); reroll.setAlignmentX(Component.CENTER_ALIGNMENT); reroll.setForeground(Color.CYAN);
            if(p.gold < rerollCost) reroll.setForeground(Color.DARK_GRAY);
            reroll.addActionListener(e -> { if(p.gold >= rerollCost) { p.gold -= rerollCost; rerollCost += 25; refreshShop(); log.append("[SYS] Shop Rerolled!\n"); } });
            items.add(reroll);
            JScrollPane shopScroll = new JScrollPane(items); shopScroll.setBorder(null); shopScroll.setBackground(new Color(20, 20, 28)); shopScroll.getViewport().setOpaque(false); shopScroll.getVerticalScrollBar().setUnitIncrement(16);
            sideShopPanel.add(shopScroll, BorderLayout.CENTER); sideShopPanel.revalidate(); sideShopPanel.repaint();
        }

        private JLabel createStatLabel(String title, String val, Color c) { JLabel l = new JLabel("<html><b>"+title+"</b> <font color='"+toHex(c)+"'>"+val+"</font></html>"); l.setFont(new Font("SansSerif", Font.PLAIN, 18)); return l; }
        private String toHex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

        private void executeAttack(String move) {
            Player p = state.player;
            int cost = move.contains("(15 EN)") ? 15 : move.contains("(20 EN)") ? 20 : move.contains("(30 EN)") ? 30 : move.contains("(35 EN)") ? 35 : move.contains("(40 EN)") ? 40 : 0;
            if(p.energy < cost) { log.append("[SYS] Not enough Energy!\n"); return; }
            inputLocked = true; setMenu("EMPTY"); p.energy -= cost; p.combo++; p.currentAnim = "WINDUP"; p.animTick = 0;
            p.onAnimComplete = () -> {
                int[] b = p.getBonusStats(); boolean isCrit = Math.random() < ((p.luk + b[4]) * 0.015); double comboMult = 1.0 + (p.combo * 0.05);
                int baseDmg = calculateBaseDamage(move, p, b);
                if(p.activeBuffTurns > 0) { baseDmg *= 1.5; } if(enemy.weakTurns > 0) { baseDmg *= 1.2; }
                for(Weapon w : p.equippedWeapons) {
                    if("Operator's Precision".equals(w.passive)) baseDmg *= 1.2;
                    if("Sorcerer's Echo".equals(w.passive) && Math.random() < 0.25) { baseDmg *= 2; log.append("[COMBAT] Sorcerer's Echo Double Cast!\n"); }
                }
                if(isCrit) baseDmg *= 2.0; int finalDmg = (int)(baseDmg * comboMult); int reducedDmg = finalDmg - enemy.def;
                if(enemy.elite == EliteModifier.ARMORED) reducedDmg -= enemy.def; if(reducedDmg < 1) reducedDmg = 1;

                enemy.takeDamage(reducedDmg); screenShake = isCrit ? 15 : 5;
                if(isCrit) { for(int i=0; i<15; i++) particles.add(new Particle(Color.YELLOW)); } else { for(int i=0; i<5; i++) particles.add(new Particle(Color.WHITE)); }
                Color dmgColor = isCrit ? Color.YELLOW : (p.combo > 3 ? Color.ORANGE : Color.WHITE);
                dmgNums.add(new DamageNumber(reducedDmg, 780, 280, dmgColor, reducedDmg + (p.combo > 3 ? "!" : "")));
                log.append("[COMBAT] " + move.split(" \\(")[0] + (isCrit ? " CRIT for " : " hits for ") + reducedDmg + " dmg (Combo x" + p.combo + ")\n");

                if(move.contains("[BURN]")) { enemy.burnTurns += 3; log.append("[COMBAT] Enemy is BURNING!\n"); }
                if(move.contains("[POISON]")) { enemy.poisonTurns += 4; log.append("[COMBAT] Enemy is POISONED!\n"); }
                if(move.contains("[WEAK]")) { enemy.weakTurns += 3; log.append("[COMBAT] Enemy is WEAKENED!\n"); }
                if(move.contains("[THORNS]")) { p.thornsTurns += 3; log.append("[COMBAT] Thorns Aura Active!\n"); }
                if(move.contains("Shield Bash") && Math.random() < 0.4) {
                    if(enemy.isBoss && Math.random() > 0.5) log.append("[COMBAT] Boss resisted STUN!\n"); else { enemy.stunned = true; log.append("[COMBAT] Enemy STUNNED!\n"); }
                }

                boolean hasVampirism = p.equippedRelics.stream().anyMatch(r -> "Vampirism".equals(r.passive)) || p.equippedWeapons.stream().anyMatch(w -> "Lifesteal".equals(w.passive));
                if(move.contains("[HEAL]") || hasVampirism) {
                    int heal = reducedDmg / (hasVampirism ? 4 : 3); p.heal(heal); dmgNums.add(new DamageNumber(heal, 280, 280, Color.GREEN));
                    for(int i=0; i<5; i++) particles.add(new Particle(Color.GREEN)); log.append("[COMBAT] Lifesteal restored " + heal + " HP!\n");
                }
                for(Weapon w : p.equippedWeapons) {
                    if("Soul Syphon".equals(w.passive)) { p.energy = Math.min(p.maxEnergy, p.energy + 15); dmgNums.add(new DamageNumber(15, 280, 260, ENERGY_BLUE, "+15 EN")); break; }
                }

                if(enemy.hp <= 0) { Timer t = new Timer(500, ev -> endEncounter()); t.setRepeats(false); t.start(); }
                else { Timer t = new Timer(500, ev -> endPlayerTurn()); t.setRepeats(false); t.start(); }
            };
        }

        private int calculateBaseDamage(String m, Player p, int[] b) {
            int tAtk = p.atk + b[1], tDef = p.def + b[2], tSpd = p.spd + b[3], tLuk = p.luk + b[4];
            if(m.contains("Basic") || m.contains("Quick Draw")) return tAtk;
            if(m.contains("Shield Bash")) return (int)(tDef * 1.8);
            if(m.contains("Aegis Crush")) return (int)(tDef * 2.5 + p.getTotalMaxHp() * 0.1);
            if(m.contains("Phalanx")) return (int)(tDef * 1.5);
            if(m.contains("Fireball")) return (int)(tAtk * 2.0);
            if(m.contains("Void Storm")) return (int)(tAtk * 3.0);
            if(m.contains("Curse")) return tAtk;
            if(m.contains("Backstab")) return (int)(tAtk + tSpd * 1.5);
            if(m.contains("Execution")) return (int)(tSpd * 2.5 + tAtk);
            if(m.contains("Toxic Dart")) return (int)(tSpd * 1.8);
            if(m.contains("Piercing Arrow")) return (int)(tSpd * 1.5 + tAtk);
            if(m.contains("Volley")) return (int)(tSpd * 2.0 + tLuk);
            if(m.contains("Snipe")) return (int)((tSpd + tAtk) * 2.5);
            if(m.contains("Holy Strike")) return (int)(tAtk * 1.5 + p.getTotalMaxHp() * 0.05);
            if(m.contains("Divine Favor")) return (int)(tAtk * 1.2 + tDef);
            if(m.contains("Smite")) return (int)(tAtk * 2.0 + tDef * 1.5);
            if(m.contains("Wind Slash")) return (int)(tAtk * 1.5 + tSpd * 2.0);
            if(m.contains("Dragon Strike")) return (int)(tAtk * 2.5 + tSpd);
            return tAtk;
        }

        private void executeDefend() {
            Player p = state.player; p.combo = 0; int bDef = p.def + p.getBonusStats()[2]; p.shield += bDef * 2 + 50;
            for(Armor a : p.equippedArmors) { if("Knight's Resolve".equals(a.passive)) p.shield += 50; }
            log.append("[COMBAT] Guarding! Shield is now " + p.shield + ".\n");
            inputLocked = true; setMenu("EMPTY"); Timer t = new Timer(500, e -> endPlayerTurn()); t.setRepeats(false); t.start();
        }

        private void executeRest() {
            Player p = state.player; if(p.gold < 10) { log.append("[SYS] Not enough gold to camp!\n"); return; }
            p.combo = 0; p.gold -= 10; int interest = (int)(p.gold * 0.05); p.gold += interest;
            p.energy = Math.min(p.maxEnergy, p.energy + 50); p.heal((int)(p.getTotalMaxHp() * 0.5));
            log.append("[COMBAT] Camped! Restored 50% HP & 50 Energy. Earned " + interest + "G interest.\n");
            buildSideShop(); inputLocked = true; setMenu("EMPTY"); Timer t = new Timer(500, e -> endPlayerTurn()); t.setRepeats(false); t.start();
        }

        private void executeFlee() {
            Player p = state.player; if(enemy.isBoss) { log.append("[SYS] Cannot flee from an Overlord!\n"); return; }
            if(p.gold < p.fleePenalty) { log.append("[SYS] Not enough gold to bribe escape!\n"); return; }
            p.combo = 0; p.winStreak = 0; p.gold -= p.fleePenalty; dmgNums.add(new DamageNumber(p.fleePenalty, 280, 250, Color.MAGENTA, "-" + p.fleePenalty + "G"));
            log.append("[SYS] Fled! Lost " + p.fleePenalty + " Gold. Win Streak Reset.\n");
            p.fleePenalty += 10; buildSideShop(); inputLocked = true; setMenu("EMPTY");
            Timer t = new Timer(1000, e -> { spawnEnemy(); inputLocked = false; setMenu("MAIN"); }); t.setRepeats(false); t.start();
        }

        private void endPlayerTurn() {
            Player p = state.player; p.energy = Math.min(p.maxEnergy, p.energy + 10); if(p.thornsTurns > 0) p.thornsTurns--;
            if(p.activeBuffTurns > 0) { p.activeBuffTurns--; if(p.activeBuffTurns == 0) log.append("[COMBAT] Damage Buff expired.\n"); }
            for(Relic r : p.equippedRelics) { if("Regeneration".equals(r.passive)) p.heal((int)(p.getTotalMaxHp() * 0.05)); if("Titan Shield".equals(r.passive)) p.shield += 15; }
            if(enemy != null && enemy.hp > 0) {
                if(p.hasPet) {
                    int petDmg = 15 + (p.level * 5); enemy.takeDamage(petDmg); log.append("[PET] Familiar attacks for " + petDmg + " dmg!\n");
                    dmgNums.add(new DamageNumber(petDmg, 780, 220, Color.CYAN)); if(enemy.hp <= 0) { endEncounter(); return; }
                }
                Timer t = new Timer(500, e -> enemyTurn()); t.setRepeats(false); t.start();
            }
        }

        private void endEncounter() {
            Player p = state.player; log.append("[SYS] ENEMY VANQUISHED\n");
            p.winStreak++; p.fleePenalty = 10; state.bountyKills++; if (enemy.isBoss) rerollCost = 50;
            if(enemy.hp < -(enemy.maxHp * 0.3)) { log.append("[SYS] OVERKILL! Bonus XP Awarded.\n"); p.xp += 50 + (enemy.level * 5); }

            int drops = enemy.elite == EliteModifier.CORRUPTED ? 2 : 1;
            for(int i=0; i<drops; i++) {
                if(enemy.isBoss || Math.random() < 0.45 || enemy.elite == EliteModifier.CORRUPTED) {
                    Equipment dropped = generateProceduralEquipment(false); boolean autoSold = false;
                    if(autoSellCommon && dropped.rarity == Rarity.COMMON) autoSold = true; if(autoSellRare && dropped.rarity == Rarity.RARE) autoSold = true; if(autoSellEpic && dropped.rarity == Rarity.EPIC) autoSold = true;
                    if(autoSold) { p.gold += dropped.rarity.sellValue; log.append("[LOOT] AUTO-SOLD " + dropped.name + " for " + dropped.rarity.sellValue + "G\n"); }
                    else { p.inventory.add(dropped); log.append("[LOOT] DROPPED: " + dropped.name + " ("+dropped.rarity.name+")\n"); }
                }
            }

            double goldMult = 1.0 + Math.min(1.5, p.winStreak * 0.05); int goldGain = (int)((enemy.isBoss ? (60 + enemy.level*20) : (15 + enemy.level*5)) * goldMult); p.gold += goldGain;
            dmgNums.add(new DamageNumber(goldGain, 650, 280, Color.YELLOW, "+" + goldGain + "G"));
            if(state.bountyKills >= state.bountyTarget) {
                p.gold += state.bountyReward; log.append("[BOUNTY] Completed! Earned " + state.bountyReward + "G\n");
                dmgNums.add(new DamageNumber(state.bountyReward, 650, 250, Color.YELLOW, "BOUNTY!"));
                state.bountyKills = 0; state.bountyTarget += 2; state.bountyReward += 50;
            }

            p.xp += enemy.isBoss ? 400 : 100; p.shield = 0; state.encounters++; encountersUntilRefresh--;
            if(encountersUntilRefresh <= 0) { refreshShop(); encountersUntilRefresh = 3; } else buildSideShop();

            Timer t = new Timer(1500, e -> {
                if (state.encounters % 5 == 0) {
                    gameLoop.stop();
                    cards.show(mainContainer, "BOSS_WARNING");
                    bossWarningScreen.play(() -> {
                        spawnEnemy(); inputLocked = false;
                        if(p.checkLevelUp(log)) { dmgNums.add(new DamageNumber(0, 250, 150, Color.CYAN, "LEVEL UP!")); setMenu("LEVEL_UP"); } else setMenu("MAIN");
                        cards.show(mainContainer, "BATTLE");
                        gameLoop.start();
                    });
                } else {
                    spawnEnemy(); inputLocked = false;
                    if(p.checkLevelUp(log)) { dmgNums.add(new DamageNumber(0, 250, 150, Color.CYAN, "LEVEL UP!")); setMenu("LEVEL_UP"); } else setMenu("MAIN");
                }
            }); t.setRepeats(false); t.start();
        }

        private void enemyTurn() {
            Player p = state.player; int dotDmg = 0;
            if(enemy.burnTurns > 0) { dotDmg += 15 + enemy.level * 2; enemy.burnTurns--; }
            if(enemy.bleedTurns > 0) { dotDmg += (int)(enemy.maxHp * 0.08); enemy.bleedTurns--; }
            if(enemy.poisonTurns > 0) { dotDmg += (int)(enemy.maxHp * 0.05); enemy.poisonTurns--; }

            if(dotDmg > 0) {
                enemy.takeDamage(dotDmg); dmgNums.add(new DamageNumber(dotDmg, 780, 250, Color.ORANGE)); log.append("[COMBAT] Enemy takes " + dotDmg + " DOT damage!\n");
                if(enemy.hp <= 0) { endEncounter(); return; }
            }

            if(enemy.weakTurns > 0) enemy.weakTurns--; if(enemy.vulnTurns > 0) enemy.vulnTurns--;
            if(enemy.freezeTurns > 0) { enemy.freezeTurns--; log.append("[COMBAT] Enemy is FROZEN and skips turn!\n"); inputLocked = false; setMenu("MAIN"); return; }
            if(enemy.stunned) { log.append("[COMBAT] Enemy is stunned and misses turn!\n"); enemy.stunned = false; inputLocked = false; setMenu("MAIN"); return; }

            enemy.turnCounter++; int[] b = p.getBonusStats(); double dodgeChance = Math.min(0.60, (p.spd + b[3]) * 0.012);
            if(Math.random() < dodgeChance) {
                log.append("[COMBAT] DODGE!\n"); dmgNums.add(new DamageNumber(0, 280, 280, Color.CYAN, "DODGE"));
                int counterDmg = (p.spd + b[3]) * 2; enemy.takeDamage(counterDmg); log.append("[COMBAT] Counter-attacked for " + counterDmg + " damage!\n");
                dmgNums.add(new DamageNumber(counterDmg, 780, 300, Color.WHITE));
                if(enemy.hp <= 0) endEncounter(); else { inputLocked = false; setMenu("MAIN"); }
                return;
            }

            enemy.currentAnim = "WINDUP"; enemy.animTick = 0;
            enemy.onAnimComplete = () -> {
                if(Math.random() < p.getParryChance()) {
                    log.append("[COMBAT] PARRY! Damage negated.\n"); dmgNums.add(new DamageNumber(0, 280, 280, Color.YELLOW, "PARRY!")); p.energy = Math.min(p.maxEnergy, p.energy + 10);
                } else {
                    double bossEnrageMult = (enemy.isBoss && enemy.turnCounter > 8) ? 1.5 : 1.0; double weaknessMult = (enemy.weakTurns > 0) ? 0.6 : 1.0;
                    int ed = (int)((enemy.atk * bossEnrageMult * weaknessMult) - (p.def + b[2])/2);
                    if(enemy.isBoss && enemy.turnCounter % 4 == 0) { ed *= 2.5; log.append("[COMBAT] OVERLORD USES DEVASTATING STRIKE!\n"); screenShake = 20; }
                    ed = Math.max(5, ed); p.takeDamage(ed); dmgNums.add(new DamageNumber(ed, 280, 280, Color.RED)); p.combo = 0;

                    if(p.thornsTurns > 0) { int refDmg = ed / 2; enemy.takeDamage(refDmg); log.append("[COMBAT] Thorns reflected " + refDmg + " damage!\n"); dmgNums.add(new DamageNumber(refDmg, 780, 280, Color.PINK)); }
                    if(enemy.elite == EliteModifier.VAMPIRIC) { enemy.heal(ed/2); }
                    if(enemy.elite == EliteModifier.TOXIC && Math.random() < 0.3) { log.append("[COMBAT] Poisoned by Toxic enemy!\n"); p.takeDamage(10); }
                }

                if(p.hp <= 0) {
                    log.append("\n[SYS] GAME OVER\n");
                    Timer deathTimer = new Timer(1500, dt -> { gameLoop.stop(); deathScreen.triggerDeath(state.encounters, state.player.level, maxComboAchieved, state.player.gold); cards.show(mainContainer, "DEATH"); }); deathTimer.setRepeats(false); deathTimer.start();
                } else { inputLocked = false; setMenu("MAIN"); }
            };
        }

        private void spawnEnemy() {
            enemy = new Enemy("Void Aberration", state.encounters, state.encounters % 5 == 0);
            log.append("\n[SYS] ENCOUNTER " + state.encounters + "\n");
            this.setBackground(TERRAIN_COLORS[state.encounters % 7]);
        }

        private Equipment generateProceduralEquipment(boolean fromMysteryBox) {
            Random rnd = new Random(); double roll = rnd.nextDouble(); double depthBonus = Math.min(0.25, state.encounters * 0.015); roll -= depthBonus; Rarity rarity;
            if(fromMysteryBox && mysteryBoxPity >= 4) { rarity = roll < 0.2 ? Rarity.GODLY : roll < 0.4 ? Rarity.MYTHIC : roll < 0.7 ? Rarity.LEGENDARY : Rarity.EPIC; mysteryBoxPity = 0; }
            else {
                rarity = roll < 0.01 ? Rarity.GODLY : roll < 0.06 ? Rarity.MYTHIC : roll < 0.22 ? Rarity.LEGENDARY : roll < 0.55 ? Rarity.EPIC : roll < 0.85 ? Rarity.RARE : Rarity.COMMON;
                if(fromMysteryBox) { if(rarity == Rarity.COMMON || rarity == Rarity.RARE) mysteryBoxPity++; else mysteryBoxPity = 0; }
            }

            int b = 2 + (state.encounters / 3) + rnd.nextInt(5); int m = (int)rarity.multiplier; int typeRoll = rnd.nextInt(3); int calcPrice = b * m * 15;
            if(typeRoll == 0) {
                String pass = rarity == Rarity.GODLY ? GODLY_PASSIVES[rnd.nextInt(GODLY_PASSIVES.length)] : (rarity == Rarity.MYTHIC ? MYTHIC_PASSIVES[rnd.nextInt(MYTHIC_PASSIVES.length)] : null);
                Relic r = new Relic(rarity.name + " " + RELIC_NAMES[rnd.nextInt(RELIC_NAMES.length)], calcPrice, b*m*5, b*m, b*m, (b/2)*m, (b/2)*m, rarity); r.passive = pass; return r;
            } else if(typeRoll == 1) {
                String name = WEAPON_PREFIXES[rnd.nextInt(WEAPON_PREFIXES.length)] + " " + WEAPON_NOUNS[rnd.nextInt(WEAPON_NOUNS.length)];
                String pass = rarity.multiplier >= 3.0 ? WEAPON_PASSIVES[rnd.nextInt(WEAPON_PASSIVES.length)] : null;
                Weapon w = new Weapon(name, calcPrice, b*m*2, (b*2)*m, (b/3)*m, b*m, b*m, rarity); w.passive = pass; return w;
            } else {
                String name = ARMOR_PREFIXES[rnd.nextInt(ARMOR_PREFIXES.length)] + " " + ARMOR_NOUNS[rnd.nextInt(ARMOR_NOUNS.length)];
                String pass = rarity.multiplier >= 5.5 ? "Knight's Resolve" : null;
                Armor a = new Armor(name, calcPrice, b*m*10, (b/3)*m, (b*2)*m, (b/2)*m, b*m, rarity); a.passive = pass; return a;
            }
        }

        private void refreshShop() {
            shopStock.clear(); shopStock.add(generateProceduralEquipment(false)); shopStock.add(generateProceduralEquipment(false));
            shopStock.add(new Consumable("Greater Potion", 50, "HEAL")); if(Math.random() < 0.5) shopStock.add(new Consumable("Damage Buff Potion", 75, "BUFF")); else shopStock.add(new Consumable("Elixir of Power", 150, "PERM_ATK"));
            shopStock.add(new Consumable("Mystery Box", 100, "GACHA")); if(state.player.level >= 3 && !state.player.hasPet) shopStock.add(new Consumable("Familiar Crystal", 300, "PET"));
            shopStock.add(new Consumable("Forge Weapon", 150, "UPGRADE")); buildSideShop();
        }

        private void drawScene(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); AffineTransform original = g.getTransform();
            if(screenShake > 0) g.translate((Math.random()-0.5)*screenShake, (Math.random()-0.5)*screenShake);
            g.setPaint(new GradientPaint(0, 0, getBackground().darker(), 0, 480, getBackground())); g.fillRect(0, 0, 950, 480);
            if(enemy != null && enemy.isBoss && enemy.turnCounter % 4 == 3) { g.setColor(new Color(255, 0, 0, 40)); g.fillRect(0, 0, 950, 480); g.setColor(Color.RED); g.setFont(new Font("Impact", Font.ITALIC, 30)); g.drawString("WARNING: DEVASTATING STRIKE", 250, 100); }

            for(Particle p : particles) p.draw(g);
            g.setColor(new Color(255,255,255,150)); g.setFont(UI_FONT); g.setColor(Color.YELLOW); g.drawString("Total Gold: " + state.player.gold + "G", 20, 30);
            g.setColor(new Color(255,255,255,150)); g.drawString("Encounter: " + state.encounters, 20, 50); g.drawString("Streak: " + state.player.winStreak, 20, 70); g.drawString("Bounty: " + state.bountyKills + "/" + state.bountyTarget, 20, 90);
            if(state.player.combo > 1) { int cSize = Math.min(45, 28 + state.player.combo * 2); g.setColor(state.player.combo > 5 ? Color.RED : XP_ORANGE); g.setFont(new Font("Impact", Font.ITALIC, cSize)); g.drawString(state.player.combo + "x COMBO", 20, 130); }

            if(enemy != null && enemy.isBoss) drawBar(g, 125, 40, "OVERLORD", enemy.displayHp, enemy.maxHp, enemy.color, 700, 35);
            state.player.render(g, 200, 250, tick); if(enemy != null && enemy.hp > 0) enemy.render(g, 650, 250, tick);
            double hpPct = state.player.displayHp / state.player.getTotalMaxHp(); Color hpCol = hpPct > 0.5 ? new Color(40, 200, 80) : hpPct > 0.2 ? Color.YELLOW : Color.RED;
            drawBar(g, 100, 380, "HP", state.player.displayHp, state.player.getTotalMaxHp(), hpCol, 300, 15);
            drawBar(g, 100, 405, "SHIELD", state.player.shield, Math.max(state.player.getTotalMaxHp(), state.player.shield), SHIELD_CYAN, 300, 15);
            drawBar(g, 100, 430, "XP", state.player.displayXp, state.player.getExpRequirement(), XP_ORANGE, 300, 15);
            drawBar(g, 100, 455, "ENERGY", state.player.displayEnergy, state.player.maxEnergy, ENERGY_BLUE, 300, 15);
            if(enemy != null && enemy.hp > 0 && !enemy.isBoss) drawBar(g, 550, 400, enemy.name, enemy.displayHp, enemy.maxHp, enemy.color, 300, 15);
            for(DamageNumber dn : dmgNums) dn.draw(g);
            g.setTransform(original);
        }

        private void drawBar(Graphics2D g, int x, int y, String label, double v, int m, Color c, int w, int h) {
            g.setColor(new Color(20, 20, 25)); g.fillRoundRect(x, y, w, h, 5, 5); g.setColor(c); g.fillRoundRect(x, y, (int)(w * Math.max(0, Math.min(1.0, v / m))), h, 5, 5);
            if(label.equals("ENERGY") && v > 0) {
                g.setColor(new Color(255, 255, 255, 100));
                for(int i=0; i<3; i++) { int px = x + (int)((tick * (2+i)) % (w * Math.max(0, Math.min(1.0, v / m)))); g.fillRect(px, y+2, 2, h-4); }
            }
            g.setColor(ACCENT_COL); g.drawRoundRect(x, y, w, h, 5, 5);
            String text = label + (m > 0 && !label.equals("SHIELD") ? ": " + (int)v + " / " + m : ": " + (int)v);
            if(w > 500) text = label;
            g.setFont(w > 500 ? new Font("Serif", Font.BOLD, 22) : new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics(); int textX = x + (w - fm.stringWidth(text)) / 2; int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
            g.setColor(Color.BLACK); g.drawString(text, textX - 1, textY - 1); g.drawString(text, textX + 1, textY - 1); g.drawString(text, textX - 1, textY + 1); g.drawString(text, textX + 1, textY + 1);
            g.setColor(Color.WHITE); g.drawString(text, textX, textY);
        }
        private void updateDmg() { dmgNums.removeIf(d -> d.life <= 0); for(DamageNumber d : dmgNums){ d.y-=2; d.life--; } }
    }

    class TitleScreen extends JPanel {
        private int tick = 0;
        public TitleScreen() {
            setLayout(null); StylizedButton s = new StylizedButton("ENTER THE RIFT"); s.setBounds(500, 600, 350, 80);
            s.setFont(new Font("SansSerif", Font.BOLD, 24)); s.addActionListener(e -> cards.show(mainContainer, "TUTORIAL")); add(s);
            new Timer(30, e -> { tick++; repaint(); }).start();
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c1 = new Color(10, 5, 15); Color c2 = new Color(40 + (int)(Math.sin(tick*0.02)*20), 10, 20);
            g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2)); g2.fillRect(0, 0, getWidth(), getHeight()); g2.setStroke(new BasicStroke(2));
            for(int i=0; i<3; i++) {
                if(Math.random() > 0.3) {
                    g2.setColor(new Color(0, 0, 0, 180)); int lx = getWidth()/2 - 400 + (int)(Math.random() * 800); int ly = 0;
                    while(ly < getHeight()) { int nx = lx + (int)(Math.random() * 60) - 30; int ny = ly + (int)(Math.random() * 80) + 20; g2.drawLine(lx, ly, nx, ny); lx = nx; ly = ny; }
                }
            }
            g2.setFont(new Font("Monospaced", Font.BOLD, 20)); g2.setColor(new Color(212, 175, 55, 40));
            for(int i=0; i<25; i++) {
                int sx = getWidth()/2 + (int)(Math.sin(tick*0.01 + i*3) * 500); int sy = getHeight()/2 + (int)(Math.cos(tick*0.015 + i*2) * 350); g2.drawString(String.valueOf((char)('\u0391' + (i%24))), sx, sy);
            }
            int floatY = (int)(Math.sin(tick * 0.05) * 15); g2.setFont(TITLE_FONT); FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth("VANGUARDS"); int titleX = (getWidth() - titleWidth) / 2; int titleY = 300 + floatY;
            g2.setColor(new Color(0, 0, 0, 150)); g2.drawString("VANGUARDS", titleX-5, titleY+5);
            g2.setColor(ACCENT_COL); g2.drawString("VANGUARDS", titleX, titleY);
            g2.setClip(new java.awt.geom.Rectangle2D.Double(titleX, titleY - fm.getAscent(), titleWidth, fm.getHeight()));
            int shineX = titleX - 200 + (tick * 15) % (titleWidth + 400); g2.setPaint(new GradientPaint(shineX, titleY, new Color(255,255,255,0), shineX+50, titleY, new Color(255,255,255,200)));
            g2.fillRect(shineX, titleY - 150, 100, 200); g2.setClip(null);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 28)); g2.setColor(Color.LIGHT_GRAY);
            int subWidth = g2.getFontMetrics().stringWidth("World's Greatest Heroes"); g2.drawString("World's Greatest Heroes", (getWidth()-subWidth)/2, 360 + floatY);
        }
    }

    class DeathScreen extends JPanel {
        private int tick = 0, score = 0, level = 0, combo = 0, gold = 0; private Timer animTimer;
        public DeathScreen() {
            setLayout(null); setBackground(Color.BLACK);
            StylizedButton restart = new StylizedButton("RESTART JOURNEY"); restart.setBounds(525, 650, 300, 60); restart.setForeground(Color.RED);
            restart.addActionListener(e -> { animTimer.stop(); cards.show(mainContainer, "TITLE"); }); add(restart);
        }
        public void triggerDeath(int encounters, int lvl, int cmbo, int gld) {
            this.score = encounters; this.level = lvl; this.combo = cmbo; this.gold = gld; this.tick = 0;
            if(animTimer != null) animTimer.stop(); animTimer = new Timer(30, e -> { tick++; repaint(); }); animTimer.start();
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int fade = Math.min(255, tick * 3); g2.setColor(new Color(fade / 3, 0, 0)); g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 50, 50, fade)); g2.setFont(new Font("Serif", Font.BOLD, 120)); FontMetrics fm = g2.getFontMetrics();
            g2.drawString("YOU DIED", (getWidth() - fm.stringWidth("YOU DIED"))/2, 250);
            if(tick > 60) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 28)); g2.setColor(Color.WHITE); g2.drawString("RUN SUMMARY", 560, 350);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 24)); g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("Encounters Cleared: " + score, 530, 420); g2.drawString("Level Reached: " + level, 530, 470);
                g2.drawString("Max Combo: " + combo + "x", 530, 520); g2.drawString("Gold Hoarded: " + gold + "G", 530, 570);
            }
        }
    }

    class ClassSelect extends JPanel {
        public ClassSelect() {
            setBackground(new Color(15, 15, 20));
            setLayout(new GridBagLayout());

            JPanel grid = new JPanel(new GridLayout(2, 3, 40, 40));
            grid.setOpaque(false);
            for(ClassType ct : ClassType.values()) {
                ClassButton b = new ClassButton("<html><center><b><font size='5'>" + ct.title + "</font></b><br><br><font size='3' color='gray'>" + ct.scaleDesc + "</font></center></html>", ct);
                b.setPreferredSize(new Dimension(220, 260));
                b.addActionListener(e -> {
                    state.player = new Player(ct); ACCENT_COL = ct.color;
                    introScreen.play(ct, () -> {
                        battlePanel.initSession();
                        cards.show(mainContainer, "BATTLE");
                    });
                    cards.show(mainContainer, "INTRO");
                });
                grid.add(b);
            }
            add(grid);
        }
    }

    class ClassButton extends StylizedButton {
        private ClassType classType;
        public ClassButton(String text, ClassType classType) {
            super(text);
            this.classType = classType;
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = 45;
            g2.setColor(classType.color);

            if(classType == ClassType.KNIGHT) { g2.fillRect(cx-15, cy-20, 30, 40); }
            else if(classType == ClassType.SORCERER) { g2.fillOval(cx-15, cy-15, 30, 30); g2.fillOval(cx-5, cy-25, 10, 10); }
            else if(classType == ClassType.OPERATOR) { g2.fillPolygon(new int[]{cx, cx+15, cx-15}, new int[]{cy+15, cy-15, cy-15}, 3); }
            else if(classType == ClassType.RANGER) { g2.drawOval(cx-15, cy-15, 30, 30); g2.drawLine(cx-15, cy+15, cx+15, cy-15); }
            else if(classType == ClassType.PALADIN) { g2.fillRect(cx-5, cy-25, 10, 40); g2.fillRect(cx-15, cy-10, 30, 10); }
            else if(classType == ClassType.RONIN) { g2.drawOval(cx-20, cy-5, 40, 10); g2.drawLine(cx-15, cy+5, cx+15, cy-20); }

            g2.dispose();
        }
        @Override protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(4));
            g2.setColor(classType.color);
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 20, 20);
            g2.dispose();
        }
    }

    class StylizedButton extends JButton {
        public StylizedButton(String text) {
            super(text);
            setContentAreaFilled(false); setFocusPainted(false); setBorder(new EmptyBorder(10, 20, 10, 20));
            setFont(UI_FONT); setForeground(TEXT_LIGHT); setCursor(new Cursor(Cursor.HAND_CURSOR)); setBackground(new Color(45, 45, 55));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(new Color(70, 70, 85)); repaint(); }
                public void mouseExited(MouseEvent e) { setBackground(new Color(45, 45, 55)); repaint(); }
                public void mousePressed(MouseEvent e) { setBackground(ACCENT_COL.darker()); repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            super.paintComponent(g); g2.dispose();
        }
        @Override protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if(getForeground().equals(Rarity.GODLY.col)) { g2.setStroke(new BasicStroke(3)); g2.setColor(new Color((int)(Math.random()*255), 255, 200)); }
            else { g2.setColor(ACCENT_COL); }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10); g2.dispose();
        }
    }

    static class GameState { Player player; int encounters = 1; int bountyKills = 0, bountyTarget = 3, bountyReward = 100; }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new Vanguards().setVisible(true)); }
}