package jl95.tbb.pmon;

import jl95.lang.I;
import jl95.lang.variadic.*;
import jl95.tbb.PartyId;
import jl95.tbb.mon.MonFieldPosition;
import jl95.util.StrictList;
import jl95.util.StrictMap;
import jl95.util.StrictSet;

import java.util.Optional;

import static jl95.lang.SuperPowers.*;

public class PlayerDialog {

    private static final int foeInfoLineAlignIndex = 32;

    public PlayerDialogInterface pli = new PlayerDialogInterface();
    private Function1<String, PartyId> partyNameGetterNullable;
    private Function1<String, Pmon.Id> pmonNameGetterNullable;
    private Function1<String, PmonMove.Id> moveNameGetterNullable;

    public PlayerDialog partyNameGetter(Function1<String, PartyId> i) {
        this.partyNameGetterNullable = i;
        return this;
    }
    public PlayerDialog pmonNameGetter(Function1<String, Pmon.Id> i) {
        this.pmonNameGetterNullable = i;
        return this;
    }
    public PlayerDialog moveNameGetter(Function1<String, PmonMove.Id> i) {
        this.moveNameGetterNullable = i;
        return this;
    }

    public StrictMap<MonFieldPosition, PmonDecision> decide(PmonLocalContext context, PartyId selfId, StrictSet<MonFieldPosition> able) {

        var partyNameGetter = Optional.ofNullable(partyNameGetterNullable).orElse(Object::toString);
        var pmonNameGetter  = Optional.ofNullable(pmonNameGetterNullable).orElse(Object::toString);
        var moveNameGetter  = Optional.ofNullable(moveNameGetterNullable).orElse(Object::toString);
        //
        var ownMonIds = I.of(context.ownParty.monsOnField.keySet()).toList();
        var outPrintField = method((Integer ownMonIdex) -> {
            for (var e : context.foeParty.entrySet()) {
                pli.outPrintAlignRight(partyNameGetter.apply(e.getKey()));
                for (var foeMon : e.getValue().values()) {
                    pli.outPrintAlignRight("%s (%s HP)".formatted(pmonNameGetter.apply(foeMon.id), foeMon.status.hp));
                }
            }
            pli.outPrintAlignLeft(partyNameGetter.apply(selfId));
            var j = 0;
            for (var k : ownMonIds) {
                var myMon = context.ownParty.monsOnField.get(k);
                pli.outPrintAlignLeft("%s %s (%s HP)".formatted(ownMonIdex == j ? ">" : " ", pmonNameGetter.apply(myMon.id), myMon.status.hp));
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
                pli.outPrintAlignLeft
                        ("What do you want to do?");
                var optionsL1 = I(
                        tuple("Fight", function(() -> new PlayerFightDialog(pli, () -> outPrintField.accept(i))
                                .moveNameGetter(moveNameGetter)
                                .decide(context, mon))),
                        tuple("Switch Out", function(() -> new PlayerSwitchDialog(pli, () -> outPrintField.accept(i))
                                .pmonNameGetter(pmonNameGetter)
                                .decide(context, mon)))
                ).<StrictList<Tuple3<Integer, String, Function0<PmonDecision>>>>apply(strict(List()), (t, l) -> l.add(tuple(l.size() + 1, t.a1, t.a2)));
                var optionsL1Map = strict(optionsL1.toMap(t -> t.a1, t -> t));
                for (var optionL1 : optionsL1) {
                    pli.outPrintAlignLeft("[%s] %s".formatted(optionL1.a1, optionL1.a2));
                }
                Function0<PmonDecision> optionL1Chosen;
                String optionL1Read = pli.getIn().nextLine();
                try {
                    optionL1Chosen = optionsL1Map.get(Integer.parseInt(optionL1Read)).a3;
                }
                catch (Exception ex) {
                    pli.outClear();
                    pli.outPrintAlignLeft("Not a valid choice: %s".formatted(optionL1Read));
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
