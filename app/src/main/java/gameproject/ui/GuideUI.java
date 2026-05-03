package gameproject.ui;

import java.awt.Color;
import java.awt.Graphics;
import gameproject.FontManager;

public class GuideUI {
    public static void draw(Graphics g, int screenWidth, int screenHeight, int currentPage) {
        g.setColor(new Color(20, 20, 30));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // Draw X Button
        int closeX = screenWidth - 80;
        int closeY = 30;
        g.setColor(Color.RED);
        g.fillRect(closeX, closeY, 50, 50);
        g.setColor(Color.WHITE);
        g.drawRect(closeX, closeY, 50, 50);
        g.setFont(FontManager.getFont(40f));
        g.drawString("X", closeX + 10, closeY + 40);

        // Draw Arrows
        g.setColor(Color.DARK_GRAY);
        g.fillRect(50, screenHeight / 2 - 50, 100, 100);
        g.fillRect(screenWidth - 150, screenHeight / 2 - 50, 100, 100);
        g.setColor(Color.WHITE);
        g.drawString("<", 85, screenHeight / 2 + 15);
        g.drawString(">", screenWidth - 115, screenHeight / 2 + 15);

        g.setColor(Color.WHITE);
        g.setFont(FontManager.getFont(50f));
        g.drawString("GAME GUIDE (" + (currentPage + 1) + "/6)", screenWidth / 2 - 250, 80);

        g.setFont(FontManager.getFont(22f));
        int startX = screenWidth / 2 - 400;
        int startY = 180;
        int lineH = 40;

        if (currentPage == 0) {
            g.setColor(Color.YELLOW);
            g.drawString("1. BASICS & CONTROLS:", startX, startY);
            g.setColor(Color.WHITE);
            g.drawString("- W, A, S, D: Move your character.", startX, startY + lineH);
            g.drawString("- Left Mouse (Hold): Shoot your weapon.", startX, startY + lineH * 2);
            g.drawString("- Shift: Dash to dodge and pass through enemies.", startX, startY + lineH * 3);
            g.drawString("- M: Toggle Large Map / Minimap.", startX, startY + lineH * 4);
            g.drawString("- I: Show Character Stats & Inventory.", startX, startY + lineH * 5);
            g.drawString("- ESC: Pause the game / Return to Menu.", startX, startY + lineH * 6);

            g.setColor(Color.YELLOW);
            g.drawString("2. GAMEPLAY LOOP:", startX, startY + lineH * 8);
            g.setColor(Color.WHITE);
            g.drawString("- Kill enemies to earn EXP. Collect them to Level Up.", startX, startY + lineH * 9);
            g.drawString("- Choose upgrades carefully to survive the increasing horde.", startX, startY + lineH * 10);
            g.drawString("- A Boss appears every 5 Waves. Defeat it for a Weapon Chest.", startX, startY + lineH * 11);
            g.drawString("- Collect Soul Stones to buy permanent upgrades in SKILLS menu.", startX, startY + lineH * 12);
        } else if (currentPage == 1) {
            g.setColor(Color.YELLOW);
            g.drawString("3. ELEMENTAL REACTIONS:", startX, startY);
            g.setColor(Color.WHITE);
            g.drawString("Enemies can be afflicted with elements (Fire, Lightning, Ice, Acid).", startX,
                    startY + lineH);

            g.setColor(new Color(255, 100, 100)); // Fire
            g.drawString("- FIRE: Burns enemies over time (DoT).", startX, startY + lineH * 3);
            g.setColor(new Color(150, 200, 255)); // Ice
            g.drawString("- ICE/CHILL: Slows enemy movement speed by 30%.", startX, startY + lineH * 4);
            g.setColor(new Color(150, 150, 255)); // Lightning
            g.drawString("- SHOCK: Applies electrical charge for reactions.", startX, startY + lineH * 5);
            g.setColor(new Color(100, 255, 100)); // Acid
            g.drawString("- POISON: Applies venom effect for reactions.", startX, startY + lineH * 6);

            g.setColor(Color.ORANGE);
            g.drawString("COMBOS (REACTIONS):", startX, startY + lineH * 8);
            g.setColor(Color.WHITE);
            g.drawString("- THERMAL SHOCK (Fire + Ice): Deals 3x ATK burst damage and FREEZES for 1.5s.", startX,
                    startY + lineH * 9);
            g.drawString("- PLASMA (Poison + Shock): Spreads heavy Plasma DoT (50% ATK) to 5 nearby enemies!", startX, startY + lineH * 10);
        } else if (currentPage == 2) {
            g.setColor(Color.YELLOW);
            g.drawString("4. WEAPON EVOLUTIONS:", startX, startY);
            g.setColor(Color.WHITE);
            g.drawString("Evolve your weapons by reaching required upgrades before opening a Boss Chest:", startX,
                    startY + lineH);

            g.setColor(new Color(255, 150, 150));
            g.drawString("HELLFIRE BOOMSTICK (Evolved Shotgun)", startX, startY + lineH * 3);
            g.setColor(Color.WHITE);
            g.drawString("Req: Shotgun + Damage Upgrade (Lv.3) + Explosive Corpse Skill (Lv.1)", startX,
                    startY + lineH * 4);

            g.setColor(new Color(150, 255, 255));
            g.drawString("RAILGUN (Evolved Assault Rifle)", startX, startY + lineH * 6);
            g.setColor(Color.WHITE);
            g.drawString("Req: Assault Rifle + Optical Scope Upgrade (Lv.3)", startX, startY + lineH * 7);

            g.setColor(new Color(200, 150, 255));
            g.drawString("LIGHTNING GUN (Evolved SMG)", startX, startY + lineH * 9);
            g.setColor(Color.WHITE);
            g.drawString("Req: SMG + Fire Rate Upgrade (Lv.3) + Chain Lightning Skill (Lv.1)", startX,
                    startY + lineH * 10);

            g.setColor(Color.LIGHT_GRAY);
            g.drawString("Note: Breakthrough Skills (Lv.1) and Stat Upgrades (Lv.3) are required.", startX,
                    startY + lineH * 12);
        } else if (currentPage == 3) {
            g.setColor(Color.YELLOW);
            g.drawString("5. WORLD EVENTS (PART 1):", startX, startY);
            g.setColor(Color.WHITE);
            g.drawString("The world is dynamic. Watch out for these events:", startX, startY + lineH);

            g.setColor(new Color(255, 50, 50));
            g.drawString("BLOOD MOON", startX, startY + lineH * 3);
            g.setColor(Color.WHITE);
            g.drawString("- Enemies enter a frenzy (+25% Speed).", startX, startY + lineH * 4);
            g.drawString("- Loot drop rates are boosted to 45% (from 25%).", startX, startY + lineH * 5);
            g.drawString("- Perfect for farming Gold and Soul Stones!", startX, startY + lineH * 6);

            g.setColor(new Color(150, 255, 100));
            g.drawString("ACID RAIN", startX, startY + lineH * 8);
            g.setColor(Color.WHITE);
            g.drawString("- Periodic damage (1 HP / 0.5s) if caught outside.", startX, startY + lineH * 9);
            g.drawString("- SEEK SHELTER inside any building to survive!", startX, startY + lineH * 10);
        } else if (currentPage == 4) {
            g.setColor(Color.YELLOW);
            g.drawString("6. WORLD EVENTS (PART 2):", startX, startY);
            
            g.setColor(new Color(100, 100, 255));
            g.drawString("DARKNESS / NIGHTFALL", startX, startY + lineH * 2);
            g.setColor(Color.WHITE);
            g.drawString("- Visibility is reduced to a small circle around you.", startX, startY + lineH * 3);
            g.drawString("- Be extra careful of projectiles and traps!", startX, startY + lineH * 4);

            g.setColor(new Color(255, 150, 50));
            g.drawString("MIMIC MANIA", startX, startY + lineH * 6);
            g.setColor(Color.WHITE);
            g.drawString("- Greed is a trap! Special chests spawn after the warning.", startX, startY + lineH * 7);
            g.drawString("- Open them quickly before the timer runs out, or they", startX, startY + lineH * 8);
            g.drawString("  will ALL awaken as deadly Mimics!", startX, startY + lineH * 9);
            g.setColor(Color.RED);
            g.drawString("- BEWARE: Even 'normal' chests have a 25% chance to bite!", startX, startY + lineH * 10);

            g.setColor(new Color(255, 200, 50));
            g.drawString("BOSS WAVES", startX, startY + lineH * 11);
            g.setColor(Color.WHITE);
            g.drawString("- Occurs every 5 Waves. Defeat them for Boss Chests.", startX, startY + lineH * 12);
        } else if (currentPage == 5) {
            g.setColor(Color.YELLOW);
            g.drawString("7. COMBO SYSTEM:", startX, startY);
            g.setColor(Color.WHITE);
            g.drawString("Kill enemies in quick succession to build your COMBO.", startX, startY + lineH);
            g.drawString("Combo resets after 3.6 seconds of no kills.", startX, startY + lineH * 2);

            g.setColor(new Color(255, 255, 100));
            g.drawString("TIER 1 (15 Kills): Move Speed +10%", startX, startY + lineH * 4);
            g.setColor(new Color(255, 165, 0));
            g.drawString("TIER 2 (30 Kills): Speed +15% | Fire Rate +5%", startX, startY + lineH * 5);
            g.setColor(new Color(255, 80, 0));
            g.drawString("TIER 3 (50 Kills): Speed +20% | Fire Rate +10%", startX, startY + lineH * 6);
            
            g.setColor(Color.CYAN);
            g.drawString("Tip: Bosses grant 20 points instantly!", startX, startY + lineH * 8);
        }
    }
}
