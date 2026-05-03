package gameproject.skill;

import java.awt.Graphics;
import java.util.ArrayList;

import gameproject.*; // Kéo Player, VFXManager từ thư mục gốc
import gameproject.entity.Enemy; // Kéo Enemy từ package entity

public interface PassiveSkill {
    void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime);

    void draw(Graphics g, Player player);

    void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime);

    default void onProjectileHit(gameproject.weapon.Projectile p, Enemy e, Player player, VFXManager vfxManager,
            long currentTime) {
    }
}