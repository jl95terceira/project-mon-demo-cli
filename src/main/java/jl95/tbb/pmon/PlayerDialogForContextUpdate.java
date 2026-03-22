package jl95.tbb.pmon;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Method0;
import jl95.lang.variadic.Method2;
import jl95.tbb.PartyId;
import jl95.tbb.mon.MonPartyFieldPosition;
import jl95.tbb.pmon.status.PmonStatusCondition;
import jl95.tbb.pmon.update.*;

import java.util.Optional;

public class PlayerDialogForContextUpdate {

    private final PlayerInterface pli = new PlayerInterface();
    private Function1<String, PartyId> partyNameGetterNullable;
    private Function1<String, Pmon.Id> pmonNameGetterNullable;
    private Function1<String, PmonMove.Id> moveNameGetterNullable;
    private Method0 pauseNullable;

    public PlayerDialogForContextUpdate partyNameGetter(Function1<String, PartyId> i) {
        this.partyNameGetterNullable = i;
        return this;
    }
    public PlayerDialogForContextUpdate pmonNameGetter(Function1<String, Pmon.Id> i) {
        this.pmonNameGetterNullable = i;
        return this;
    }
    public PlayerDialogForContextUpdate moveNameGetter(Function1<String, PmonMove.Id> i) {
        this.moveNameGetterNullable = i;
        return this;
    }
    public PlayerDialogForContextUpdate pause(Method0 i) {
        this.pauseNullable = i;
        return this;
    }

    public void handle(PmonLocalContext context, PmonUpdate pmonUpdate) {

        var partyNameGetter = Optional.ofNullable(partyNameGetterNullable).orElse(Object::toString);
        var pmonNameGetter  = Optional.ofNullable(pmonNameGetterNullable).orElse(Object::toString);
        var moveNameGetter  = Optional.ofNullable(moveNameGetterNullable).orElse(Object::toString);
        var pause           = Optional.ofNullable(pauseNullable).orElse(() -> {});
        //
        pli.outClear();
        Method2<MonPartyFieldPosition,Iterable<PmonUpdateOnTarget>> updateOnTargetHandler = (monId, atomicUpdates) -> {
            var targetPartyName = partyNameGetter.apply(monId.partyId());
            var targetMonName = pmonNameGetter.apply(context.allParties.get(monId.partyId()).get(monId.position()).id);
            for (var atomicUpdate: atomicUpdates) {
                atomicUpdate.get(new PmonUpdateOnTarget.Handler() {
                    @Override
                    public void damage(PmonUpdateOnTargetByDamage update) {
                        if (update.criticalHit) {
                            pli.outPrintAlignLeft("It's a critical hit!");
                            pause.accept();
                        }
                        if (update.effectivenessFactor != 1.0) {
                            pli.outPrintAlignLeft(update.effectivenessFactor > 1.0? "It's super effective!": "It's not very effective...");
                            pause.accept();
                        }
                        pli.outPrintAlignLeft("%s's %s took %s damage!%n".formatted(targetPartyName, targetMonName, update.damage));
                        pause.accept();
                    }

                    @Override
                    public void statModify(PmonUpdateOnTargetByStatModifier update) {
                        for (var e: update.increments.entrySet()) {
                            pli.outPrintAlignLeft("%s's %s got its %s %s!%n".formatted(targetPartyName, targetMonName, e.getKey(), e.getValue() > 0? "increased": "reduced"));
                            pause.accept();
                        }
                        for (var e: update.resets) {
                            pli.outPrintAlignLeft("%s's %s got its %s reset!%n".formatted(targetPartyName, targetMonName, e));
                            pause.accept();
                        }
                    }

                    @Override
                    public void statusCondition(PmonUpdateOnTargetByStatusCondition update) {
                        for (var e: update.statusConditionsInflict) {
                            pli.outPrintAlignLeft("%s's %s attained %s!%n".formatted(targetPartyName, targetMonName, e.id));
                            pause.accept();
                        }
                        for (var e: update.statusConditionsCure) {
                            pli.outPrintAlignLeft("%s's %s was cured of %s!%n".formatted(targetPartyName, targetMonName, e));
                            pause.accept();
                        }
                    }

                    @Override
                    public void lockMove(PmonUpdateOnTargetByLockMove pmonUpdateOnTargetByLockMove) {
                        pli.outPrintAlignLeft("%s's %s was move-locked!%n".formatted(targetPartyName, targetMonName));
                        pause.accept();
                    }

                    @Override
                    public void switchOut(PmonUpdateOnTargetBySwitchOut update) {
                        pli.outPrintAlignLeft("???%n");
                        pause.accept();
                    }
                });
            }
        };
        pmonUpdate.get(new PmonUpdate.Handler() {
            @Override
            public void pass(PmonUpdateByPass update) {
                var party = context.ownParty;
                pli.outPrintAlignLeft("%s's %s won't do anything!\n".formatted(
                        partyNameGetter.apply(update.partyId),
                        pmonNameGetter.apply(party.monsOnField.get(update.monFieldPosition).id)));
                pause.accept();
            }

            @Override
            public void switchOut(PmonUpdateBySwitchOut update) {
                var party = context.allParties.get(update.partyId);
                pli.outPrintAlignLeft("%s withdraws %s and switches in %s!\n".formatted(
                        partyNameGetter.apply(update.partyId),
                        pmonNameGetter.apply(party.get(update.monFieldPosition).id),
                        pmonNameGetter.apply(update.monToSwitchInId)));
                pause.accept();
            }

            @Override
            public void move(PmonUpdateByMove update) {
                var party = context.allParties.get(update.monPartyFieldPosition.partyId());
                var mon = party.get(update.monPartyFieldPosition.position());
                pli.outPrintAlignLeft("%s's %s used %s!\n".formatted(
                        partyNameGetter.apply(update.monPartyFieldPosition.partyId()),
                        pmonNameGetter.apply(mon.id),
                        moveNameGetter.apply(update.moveId)));
                pause.accept();
                for (var e: update.usageResults) {
                    var foePartyId = e.a1;
                    var foeMonPosition = e.a2;
                    var foeMon = context.allParties.get(foePartyId).get(foeMonPosition);
                    e.a3.get(new PmonUpdateByMove.UsageResult.Handler() {
                        @Override
                        public void miss(PmonUpdateByMove.UsageResult.MissType missType) {
                            pli.outPrintAlignLeft("It missed %s's %s...%n".formatted(partyNameGetter.apply(foePartyId), pmonNameGetter.apply(foeMon.id)));
                            pause.accept();
                        }
                        @Override
                        public void immobilised(PmonStatusCondition.Id id) {
                            pli.outPrintAlignLeft("%s's %s is immobilised!%n".formatted(partyNameGetter.apply(update.monPartyFieldPosition.partyId()), pmonNameGetter.apply(mon.id)));
                            pause.accept();
                        }
                        @Override
                        public void hit(Iterable<PmonUpdateOnTarget> atomicUpdates) {
                            updateOnTargetHandler.accept(new MonPartyFieldPosition(foePartyId, foeMonPosition), atomicUpdates);
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
}
