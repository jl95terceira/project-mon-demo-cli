package jl95.tbb.pmon.demo;

import jl95.lang.I;
import jl95.lang.Ref;
import jl95.lang.variadic.Function0;
import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Method1;
import jl95.lang.variadic.Method2;
import jl95.tbb.Battle;
import jl95.tbb.PartyId;
import jl95.tbb.mon.MonFieldPosition;
import jl95.tbb.mon.MonId;
import jl95.tbb.pmon.*;
import jl95.tbb.pmon.decision.PmonDecisionToSwitchOut;
import jl95.tbb.pmon.decision.PmonDecisionToUseMove;
import jl95.tbb.pmon.effect.PmonEffects;
import jl95.tbb.pmon.status.PmonStatModifierType;
import jl95.tbb.pmon.status.PmonStatusCondition;
import jl95.tbb.pmon.update.*;
import jl95.util.StrictMap;
import jl95.util.StrictSet;

import java.util.Map;
import java.util.Random;

import static jl95.lang.SuperPowers.*;

public class Main {

    public static class PartyIds {
        public static StrictMap<PartyId, String> namesMap = strict(Map());
        public static PartyId named(String name) {
            var id = new PartyId();
            namesMap.put(id, name);
            return id;
        }
        public static PartyId PLAYER1 = named("Player 1");
        public static PartyId PLAYER2 = named("Player 2");
    }
    public static class Pmons {
        public static StrictMap<Pmon.Id, String> namesMap = strict(Map());
        public static Pmon.Id named(String name) {
            var id = new Pmon.Id();
            namesMap.put(id, name);
            return id;
        }
        public static Pmon pmon1 = new Pmon(named("RED"));
        public static Pmon pmon2 = new Pmon(named("BLUE"));
        public static Pmon pmon3 = new Pmon(named("GREEN"));
        public static Pmon pmon4 = new Pmon(named("YELLOW"));
    }
    public static class PmonTypes {
        public static PmonType NORMAL = new PmonType(new PmonType.Id()) {
            @Override
            public PmonMove.EffectivenessType effectivenessAgainst(PmonType other) {
                return PmonMove.EffectivenessType.NORMAL;
            }
        };
    }
    public static class MoveFactories {
        public static StrictMap<PmonMove.Id, String> namesMap = strict(Map());
        public static PmonMove.Id named(String name) {
            var id = new PmonMove.Id();
            namesMap.put(id, name);
            return id;
        }
        private static Function1<PmonEffects, PmonMove.EffectsContext> build(Method1<PmonEffects> builder) {
            return ctx -> {
                var e = new PmonEffects();
                builder.accept(e);
                return e;
            };
        }
        public static Function0<PmonMove> tackle = () -> {
            var move = new PmonMove(named("Tackle"), PmonTypes.NORMAL);
            move.status.pp = 30;
            move.accuracy = 80;
            move.effectsOnTarget = build(effects -> {
                effects.damage.power = PmonMove.Power.typed(40);
                effects.damage.pmonType = PmonTypes.NORMAL;
            });
            return move;
        };
        public static Function0<PmonMove> growl  = () -> {
            var move = new PmonMove(named("Growl"), PmonTypes.NORMAL);
            move.status.pp = 20;
            move.accuracy = 100;
            move.effectsOnTarget = build(effects -> {
                effects.stats.statModifiers.put(PmonStatModifierType.ATTACK, new Chanced<>(-1, 100));
            });
            return move;
        };
        public static Function0<PmonMove> leer   = () -> {
            var move = new PmonMove(named("Leer"), PmonTypes.NORMAL);
            move.status.pp = 20;
            move.accuracy = 100;
            move.effectsOnTarget = build(effects -> {
                effects.stats.statModifiers.put(PmonStatModifierType.DEFENSE, new Chanced<>(-1, 100));
            });
            return move;
        };
    }

    static {
        for (var pmon: I(Pmons.pmon1, Pmons.pmon2, Pmons.pmon3, Pmons.pmon4)) {
            pmon.baseStats.hp = 95;
            pmon.baseStats.attack = 55;
            pmon.baseStats.defense = 40;
            pmon.baseStats.speed = 90;
            pmon.restoreHp();
        }
        Pmons.pmon1.moves.add(MoveFactories.tackle.apply());
        Pmons.pmon1.moves.add(MoveFactories.growl .apply());
        Pmons.pmon2.moves.add(MoveFactories.tackle.apply());
        Pmons.pmon2.moves.add(MoveFactories.leer  .apply());
        Pmons.pmon3.moves.add(MoveFactories.tackle.apply());
        Pmons.pmon3.moves.add(MoveFactories.growl .apply());
        Pmons.pmon4.moves.add(MoveFactories.tackle.apply());
        Pmons.pmon4.moves.add(MoveFactories.leer  .apply());
    }

    public static void pause() { sleep(600); }
    public static void main(String[] args) {

        var battle = new PmonBattle(new PmonRuleset());
        var playerEntry = new PmonPartyEntry();
        playerEntry.mons.addAll(List(Pmons.pmon1, Pmons.pmon4));
        var npcEntry = new PmonPartyEntry();
        npcEntry.mons.addAll(List(Pmons.pmon2, Pmons.pmon3));
        Ref<PmonGlobalContext> globalContextRef = new Ref<>();
        StrictMap<PartyId, PmonLocalContext> localContextRefs = strict(Map());
        Method2<MonId,Iterable<PmonUpdateOnTarget>> updateOnTargetHandler = (monId, atomicUpdates) -> {
            var foePartyName = PartyIds.namesMap.get(monId.partyId());
            var foeMonName = Pmons.namesMap.get(globalContextRef.get().parties.get(monId.partyId()).monsOnField.get(monId.position()).id);
            for (var atomicUpdate: atomicUpdates) {
                atomicUpdate.get(new PmonUpdateOnTarget.Handler() {
                    @Override
                    public void damage(PmonUpdateOnTargetByDamage update) {
                        if (update.criticalHit) {
                            System.out.println("It's a critical hit!");
                        }
                        if (update.effectivenessFactor != 1.0) {
                            System.out.println(update.effectivenessFactor > 1.0? "It's super effective!": "It's not very effective...");
                        }
                        System.out.printf("%s's %s took %s damage!%n", foePartyName, foeMonName, update.damage);
                    }

                    @Override
                    public void statModify(PmonUpdateOnTargetByStatModifier update) {
                        System.out.printf("%s's %s got its stats modified!%n", foePartyName, foeMonName);
                    }

                    @Override
                    public void statusCondition(PmonUpdateOnTargetByStatusCondition update) {
                        System.out.printf("%s's %s attained a status condition!%n", foePartyName, foeMonName);
                    }

                    @Override
                    public void lockMove(PmonUpdateOnTargetByLockMove pmonUpdateOnTargetByLockMove) {
                        System.out.printf("%s's %s was move-locked!%n", foePartyName, foeMonName);
                    }

                    @Override
                    public void switchOut(PmonUpdateOnTargetBySwitchOut update) {
                        System.out.printf("???%n");
                    }
                });
            }
        };
        var winner = battle.start(
                strict(Map(
                        tuple(PartyIds.PLAYER1, playerEntry),
                        tuple(PartyIds.PLAYER2, npcEntry))),
                new PmonInitialConditions(),
                function((StrictMap<PartyId, StrictSet<MonFieldPosition>> monPositionsAbleMap) -> strict(I.of(monPositionsAbleMap.entrySet())
                        .toMap(Map.Entry::getKey, e -> function((PartyId p, StrictSet<MonFieldPosition> monPositionsAble) -> {

                    var partyName = PartyIds.namesMap.get(p);
                    var pFoe = localContextRefs.get(p).foeParty.keySet().iterator().next(); // only 1 foe in this demo (1v1) so get single next
                    var party = globalContextRef.get().parties.get(p);
                    var decision = function((Pmon mon) -> {

                        if (mon.status.hp <= 0 || I.of(mon.status.statModifiers.values()).any(stages -> stages <= -2)) {
                            var decisionToSwitchIn = new PmonDecisionToSwitchOut();
                            for (var i : I.range(party.mons.size())) {
                                var monToSwitchIn = party.mons.get(i);
                                if (monToSwitchIn.status.hp <= 0) continue;
                                if (monToSwitchIn == mon) continue;
                                decisionToSwitchIn.monSwitchInIndex = i;
                                return PmonDecision.from(decisionToSwitchIn);
                            }
                            // no switch-in-able mon in party
                        }
                        var decisionToUseMove = new PmonDecisionToUseMove();
                        decisionToUseMove.moveIndex = new Random().nextInt(0, mon.moves.size());
                        decisionToUseMove.target = PmonDecisionToUseMove.Target.mon(new MonId(pFoe, localContextRefs.get(p).foeParty.get(pFoe).keySet().iterator().next()));
                        return PmonDecision.from(decisionToUseMove);
                    });
                    return strict(I.of(monPositionsAble).toMap(id -> id, id -> {

                        var mon = party.monsOnField.get(id);
                        System.out.println(partyName + " is making a decision for " + Pmons.namesMap.get(mon.id) + " ...");
                        pause();
                        var d = decision.apply(mon);
                        System.out.println(partyName + " has decided!");
                        pause();
                        return d;
                    }));
                }).apply(e.getKey(), e.getValue())))),
                new Battle.Handler<>() {
                    @Override
                    public void onGlobalContext(PmonGlobalContext context) {

                        globalContextRef.set(context);
                    }

                    @Override
                    public void onLocalContext(PartyId p, PmonLocalContext localContext) {

                        localContextRefs.put(p, localContext);
                        if (p != PartyIds.PLAYER1) return;
                        System.out.println(PartyIds.namesMap.get(p));
                        for (var mon: localContextRefs.get(p).ownParty.monsOnField.values()) {
                            System.out.println("    "+Pmons.namesMap.get(mon.id)+" (%s HP)".formatted(mon.status.hp));
                        }
                        for (var eFoe: localContextRefs.get(p).foeParty.entrySet()) {
                            var pFoe = eFoe.getKey();
                            var foe = eFoe.getValue();
                            System.out.println(PartyIds.namesMap.get(pFoe));
                            for (var mon: foe.values()) {
                                System.out.println("    "+Pmons.namesMap.get(mon.id)+" (%s HP)".formatted(mon.status.hp));
                            }
                        }
                        pause();
                    }

                    @Override
                    public void onLocalUpdate(PartyId id, PmonUpdate pmonUpdate) {

                        if (id != PartyIds.PLAYER1) return;
                        pmonUpdate.get(new PmonUpdate.Handler() {
                            @Override
                            public void pass(PmonUpdateByPass update) {
                                var party = globalContextRef.get().parties.get(update.partyId);
                                System.out.printf("%s's %s won't do anything!\n",
                                        PartyIds.namesMap.get(update.partyId),
                                        Pmons.namesMap.get(party.monsOnField.get(update.monFieldPosition).id)); pause();
                            }

                            @Override
                            public void switchOut(PmonUpdateBySwitchOut update) {
                                var party = globalContextRef.get().parties.get(update.partyId);
                                System.out.printf("%s withdraws %s and switches in %s!\n",
                                        PartyIds.namesMap.get(update.partyId),
                                        Pmons.namesMap.get(party.monsOnField.get(update.monFieldPosition).id),
                                        Pmons.namesMap.get(party.mons.get(update.monToSwitchInPartyPosition).id)); pause();
                            }

                            @Override
                            public void move(PmonUpdateByMove update) {
                                var party = globalContextRef.get().parties.get(update.monId.partyId());
                                var mon = party.monsOnField.get(update.monId.position());
                                System.out.printf("%s's %s used %s!\n",
                                        PartyIds.namesMap.get(update.monId.partyId()),
                                        Pmons.namesMap.get(mon.id),
                                        MoveFactories.namesMap.get(mon.moves.get(update.moveIndex).id)); pause();
                                for (var e: update.usageResults) {
                                    var foePartyId = e.a1;
                                    var foeMonPosition = e.a2;
                                    var foeMon = globalContextRef.get().parties.get(foePartyId).monsOnField.get(foeMonPosition);
                                    e.a3.get(new PmonUpdateByMove.UsageResult.Handler() {
                                        @Override
                                        public void miss(PmonUpdateByMove.UsageResult.MissType missType) {
                                            System.out.printf("Wow... It missed %s's %s!%n", PartyIds.namesMap.get(foePartyId), Pmons.namesMap.get(foeMon.id));
                                        }
                                        @Override
                                        public void immobilised(PmonStatusCondition.Id id) {
                                            System.out.printf("%s's %s is immobilised!%n", PartyIds.namesMap.get(update.monId.partyId()), Pmons.namesMap.get(mon.id));
                                        }
                                        @Override
                                        public void hit(Iterable<PmonUpdateOnTarget> atomicUpdates) {
                                            updateOnTargetHandler.accept(new MonId(foePartyId, foeMonPosition), atomicUpdates);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void other(PmonUpdateByOther pmonUpdateByOther) {
                                for (var e: pmonUpdateByOther.atomicUpdates.entrySet()) {
                                    updateOnTargetHandler.accept(e.getKey(), e.getValue());
                                }
                            }
                        });
                    }
                },
                constant(false)
        );
        if (winner.isPresent()) {
            System.out.printf("%s is the winner!%n", PartyIds.namesMap.get(winner.get()));
        }
        else {
            System.out.printf("Nobody wins%n");
        }
    }
}
