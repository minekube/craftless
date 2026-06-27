# Bounded Attack Exploration Design

## Intent

The 2026-06-27 final gameplay run reached the human-ready window, but the
process-external public agent reported
`insufficient-public-evidence:entity.query.attack-target`. Evidence showed the
agent had crafted a `Wooden Sword`, then explored through generated navigation
and entity perception, but the bounded attack search stopped after a single
waypoint ring that only exposed dropped items and vertically distant aquatic
entities.

This phase improves external public-agent policy only. It must not add
`find.cow`, `kill.cow`, `hunt.animal`, a survival macro, or any new product
gameplay action.

## Product Rules

- Keep combat target discovery on generated `entity.query` results.
- Keep movement on generated `navigation.plan`, `navigation.follow`, and the
  existing generic fallback `player.move` inside focus logic.
- Accept only public combat evidence targets whose labels are already treated
  as completion-valid, such as Cow, Pig, Sheep, Chicken, or Zombie.
- Continue ignoring generic aquatic targets such as Cod, Salmon, and Squid as
  completion evidence unless a future spec defines aquatic combat evidence.
- Preserve explicit blockers when bounded generated exploration finds no valid
  public attack target.

## Behavior

When the local attack-target query finds no valid target:

1. Query player position.
2. Generate multiple bounded exploration rings around that origin.
3. Navigate to each waypoint through generated navigation actions.
4. Query public entities after each successful waypoint.
5. Skip non-evidence aquatic living entities and object drops.
6. Stop at the first valid public attack target.
7. Return `insufficient-public-evidence:entity.query.attack-target` if bounded
   exploration finds none.

The bounded rings are external agent policy. They increase search persistence
without increasing Craftless product API breadth.

## Evidence

Tests and live artifacts must show:

- the public agent continues beyond the first waypoint ring;
- exploration still uses generated navigation and entity queries only;
- aquatic entities are not attacked as completion evidence;
- no `task.survival.*`, `find.cow`, `kill.cow`, or scenario shortcut appears;
- the final gameplay gate either reaches `publicAgentState=RAN` or reports a
  precise generic evidence blocker.
