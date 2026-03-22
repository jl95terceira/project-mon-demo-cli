package jl95.tbb.pmon;

import jl95.lang.I;
import jl95.lang.variadic.*;
import jl95.tbb.PartyId;
import jl95.tbb.mon.MonFieldPosition;
import jl95.tbb.mon.MonId;
import jl95.tbb.pmon.decision.PmonDecisionToSwitchOut;
import jl95.tbb.pmon.decision.PmonDecisionToUseMove;
import jl95.util.StrictList;
import jl95.util.StrictMap;
import jl95.util.StrictSet;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

import static jl95.lang.SuperPowers.*;

public class PlayerCharacterDialog {

    private static final int foeInfoLineAlignIndex = 32;

    private PrintStream outNullable;
    private InputStream inNullable;
    private Function1<String, PartyId> partyNameGetterNullable;
    private Function1<String, Pmon.Id> pmonNameGetterNullable;
    private Function1<String, PmonMove.Id> moveNameGetterNullable;

    public PlayerCharacterDialog out(PrintStream out) {
        this.outNullable = out;
        return this;
    }
    public PlayerCharacterDialog in(InputStream in) {
        this.inNullable = in;
        return this;
    }
    public PlayerCharacterDialog partyNameGetter(Function1<String, PartyId> i) {
        this.partyNameGetterNullable = i;
        return this;
    }
    public PlayerCharacterDialog pmonNameGetter(Function1<String, Pmon.Id> i) {
        this.pmonNameGetterNullable = i;
        return this;
    }
    public PlayerCharacterDialog moveNameGetter(Function1<String, PmonMove.Id> i) {
        this.moveNameGetterNullable = i;
        return this;
    }

    private Method1<Object> getOutPrinterAlignLeft(PrintStream out) {
        return infoAsObject -> out.printf("%s%n", infoAsObject.toString());
    }
    private Method1<Object> getOutPrinterAlignRight(PrintStream out) {
        return infoAsObject -> {
            var info = infoAsObject.toString();
            out.printf("%s%s%n", " ".repeat(Math.max(foeInfoLineAlignIndex - info.length(), 0)), info);
        };
    }

    public StrictMap<MonFieldPosition, PmonDecision> decide(PmonLocalContext context, PartyId selfId, StrictSet<MonFieldPosition> able) {
        var out             = Optional.ofNullable(outNullable).orElse(System.out);
        var outClear        = method(() -> out.print("\033\143"));
        var in              = new Scanner(Optional.ofNullable(inNullable).orElse(System.in));
        var partyNameGetter = Optional.ofNullable(partyNameGetterNullable).orElse(Object::toString);
        var pmonNameGetter  = Optional.ofNullable(pmonNameGetterNullable).orElse(Object::toString);
        var moveNameGetter  = Optional.ofNullable(moveNameGetterNullable).orElse(Object::toString);
        //
        var outPrintAlignLeft = getOutPrinterAlignLeft(out);
        var outPrintAlignRight = getOutPrinterAlignRight(out);
        var ownMonIds = I.of(context.ownParty.monsOnField.keySet()).toList();
        var outPrintField = method((Integer ownMonIdex) -> {
            for (var e : context.foeParty.entrySet()) {
                outPrintAlignRight.accept(partyNameGetter.apply(e.getKey()));
                for (var foeMon : e.getValue().values()) {
                    outPrintAlignRight.accept("%s (%s HP)".formatted(pmonNameGetter.apply(foeMon.id), foeMon.status.hp));
                }
            }
            outPrintAlignLeft.accept(partyNameGetter.apply(selfId));
            var j = 0;
            for (var k : ownMonIds) {
                var myMon = context.ownParty.monsOnField.get(k);
                outPrintAlignLeft.accept("%s %s (%s HP)".formatted(ownMonIdex == j ? ">" : " ", pmonNameGetter.apply(myMon.id), myMon.status.hp));
                j += 1;
            }
        });
        StrictMap<MonFieldPosition, PmonDecision> decisionsMap = strict(Map());
        for (var i: I.range(ownMonIds.size())) {
            MonFieldPosition monId = ownMonIds.get(i);
            if (!able.contains(monId)) {
                continue;
            }
            Pmon mon = context.ownParty.monsOnField.get(monId);
            PmonDecision decision = null;
            do {
                outPrintField.accept(i);
                outPrintAlignLeft.accept("What do you want to do?");
                var optionsL1 = I(
                        tuple("Fight", function(() -> {
                            outClear.accept();
                            final var GO_BACK = -1;
                            final StrictList<Tuple2<Integer, PmonMove>> movesOrdered = I.of(mon.moves).apply(strict(List()), (move, l) -> l.add(tuple(l.size()+1, move)));
                            PmonDecision decision_ = null;
                            do {
                                outPrintField.accept(i);
                                outPrintAlignLeft.accept("Choose move (or %s to go back)".formatted(GO_BACK));
                                for (var moveOrdered: movesOrdered) {
                                    outPrintAlignLeft.accept("[%s] %s".formatted(moveOrdered.a1, moveNameGetter.apply(moveOrdered.a2.id)));
                                }
                                String moveRead = in.nextLine();
                                try {
                                    int moveIndex = Integer.parseInt(moveRead);
                                    if (moveIndex == GO_BACK) {
                                        outClear.accept();
                                        return null;
                                    }
                                    var useMove = new PmonDecisionToUseMove();
                                    useMove.moveIndex = moveIndex - 1;
                                    if (useMove.moveIndex < 0 || useMove.moveIndex >= movesOrdered.size()) {
                                        outClear.accept();
                                        outPrintAlignLeft.accept("Invalid index for move: %s".formatted(moveIndex));
                                        continue;
                                    }
                                    useMove.target = PmonDecisionToUseMove.Target.mon(new MonId(context.foeParty.keySet().iterator().next(), context.foeParty.values().iterator().next().keySet().iterator().next())); //TODO: fix this - works only in case of foe 1 party with 1 mon
                                    decision_ = PmonDecision.from(useMove);
                                } catch (Exception ex) {
                                    outClear.accept();
                                    outPrintAlignLeft.accept("Not a valid choice: %s".formatted(moveRead));
                                }
                            }
                            while (decision_ == null);
                            return decision_;
                        })),
                        tuple("Switch Out", function(() -> {
                            final var GO_BACK = -1;
                            PmonDecision decision_ = null;
                            do {
                                outPrintField.accept(i);
                                outPrintAlignLeft.accept("Choose mon to switch in (or %s to go back)".formatted(GO_BACK));
                                int j = 1;
                                for (var waitingMon: context.ownParty.mons) {
                                    outPrintAlignLeft.accept("[%s] %s (%s HP)".formatted(j, pmonNameGetter.apply(waitingMon.id), waitingMon.status.hp));
                                    j += 1;
                                }
                                String monSwitchRead = in.nextLine();
                                try {
                                    int monSwitchInIndex = Integer.parseInt(monSwitchRead);
                                    if (monSwitchInIndex == -1) {
                                        outClear.accept();
                                        return null;
                                    }
                                    var switchOut = new PmonDecisionToSwitchOut();
                                    switchOut.monSwitchInIndex = monSwitchInIndex - 1;
                                    if (switchOut.monSwitchInIndex < 0 || switchOut.monSwitchInIndex >= context.ownParty.mons.size()) {
                                        outClear.accept();
                                        outPrintAlignLeft.accept("Invalid index for switch-in: %s".formatted(monSwitchInIndex));
                                        continue;
                                    }
                                    else if (context.ownParty.mons.get(switchOut.monSwitchInIndex) == mon) {
                                        outClear.accept();
                                        outPrintAlignLeft.accept("Cannot switch-in a mon that already is on field");
                                        continue;
                                    }
                                    decision_ = PmonDecision.from(switchOut);
                                }
                                catch (Exception ex) {
                                    outClear.accept();
                                    outPrintAlignLeft.accept("Not a valid choice: %s".formatted(monSwitchRead));
                                }
                            }
                            while (decision_ == null);
                            return decision_;
                        }))
                ).<StrictList<Tuple3<Integer, String, Function0<PmonDecision>>>>apply(strict(List()), (t, l) -> l.add(tuple(l.size() + 1, t.a1, t.a2)));
                var optionsL1Map = strict(optionsL1.toMap(t -> t.a1, t -> t));
                for (var optionL1 : optionsL1) {
                    outPrintAlignLeft.accept("[%s] %s".formatted(optionL1.a1, optionL1.a2));
                }
                Function0<PmonDecision> optionL1Chosen = null;
                String optionL1Read = in.nextLine();
                try {
                    optionL1Chosen = optionsL1Map.get(Integer.parseInt(optionL1Read)).a3;
                }
                catch (Exception ex) {
                    outClear.accept();
                    outPrintAlignLeft.accept("Not a valid choice: %s".formatted(optionL1Read));
                    continue;
                }
                decision = optionL1Chosen.apply();
            }
            while (decision == null);
            decisionsMap.put(monId, decision);
        }
        return decisionsMap;
    }
}
