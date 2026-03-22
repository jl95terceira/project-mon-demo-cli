package jl95.tbb.pmon;

import jl95.lang.variadic.Function1;
import jl95.lang.variadic.Method0;
import jl95.tbb.pmon.decision.PmonDecisionToSwitchOut;

import java.util.Optional;

public class PlayerSwitchDialog {

    private final PlayerDialogInterface pli;
    private final Method0 fieldPrinter;
    private Function1<String, Pmon.Id> pmonNameGetterNullable;

    public PlayerSwitchDialog(PlayerDialogInterface pli,
                              Method0 fieldPrinter) {
        this.pli = pli;
        this.fieldPrinter = fieldPrinter;
    }

    public PlayerSwitchDialog pmonNameGetter(Function1<String, Pmon.Id> i) {
        this.pmonNameGetterNullable = i;
        return this;
    }
    public PmonDecision decide(PmonLocalContext context, Pmon mon) {

        var pmonNameGetter  = Optional.ofNullable(pmonNameGetterNullable).orElse(Object::toString);
        //
        final var GO_BACK = -1;
        PmonDecision decision_ = null;
        do {
            fieldPrinter.accept();
            pli.outPrintAlignLeft("Choose mon to switch in (or %s to go back)".formatted(GO_BACK));
            int j = 1;
            for (var waitingMon: context.ownParty.mons) {
                pli.outPrintAlignLeft("[%s] %s (%s HP)".formatted(j, pmonNameGetter.apply(waitingMon.id), waitingMon.status.hp));
                j += 1;
            }
            String monSwitchRead = pli.getIn().nextLine();
            try {
                int monSwitchInIndex = Integer.parseInt(monSwitchRead);
                if (monSwitchInIndex == -1) {
                    pli.outClear();
                    return null;
                }
                var switchOut = new PmonDecisionToSwitchOut();
                switchOut.monSwitchInIndex = monSwitchInIndex - 1;
                if (switchOut.monSwitchInIndex < 0 || switchOut.monSwitchInIndex >= context.ownParty.mons.size()) {
                    pli.outClear();
                    pli.outPrintAlignLeft("Invalid index for switch-in: %s".formatted(monSwitchInIndex));
                    continue;
                }
                else if (context.ownParty.mons.get(switchOut.monSwitchInIndex) == mon) {
                    pli.outClear();
                    pli.outPrintAlignLeft("Cannot switch-in a mon that already is on field");
                    continue;
                }
                decision_ = PmonDecision.from(switchOut);
            }
            catch (Exception ex) {
                pli.outClear();
                pli.outPrintAlignLeft("Not a valid choice: %s".formatted(monSwitchRead));
            }
        }
        while (decision_ == null);
        return decision_;
    }
}
