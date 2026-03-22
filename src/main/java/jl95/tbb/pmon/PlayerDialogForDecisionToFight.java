package jl95.tbb.pmon;

import jl95.lang.I;
import jl95.lang.variadic.*;
import jl95.tbb.mon.MonPartyFieldPosition;
import jl95.tbb.pmon.decision.PmonDecisionToUseMove;
import jl95.util.StrictList;

import java.util.Optional;

import static jl95.lang.SuperPowers.*;

public class PlayerDialogForDecisionToFight {

    private final PlayerInterface pli;
    private final Method0 fieldPrinter;
    private Function1<String, PmonMove.Id> moveNameGetterNullable;

    public PlayerDialogForDecisionToFight(PlayerInterface pli,
                                          Method0 fieldPrinter) {
        this.pli = pli;
        this.fieldPrinter = fieldPrinter;
    }

    public PlayerDialogForDecisionToFight moveNameGetter(Function1<String, PmonMove.Id> i) {
        this.moveNameGetterNullable = i;
        return this;
    }
    public PmonDecision decide(PmonLocalContext context, Pmon mon) {

        var moveNameGetter  = Optional.ofNullable(moveNameGetterNullable).orElse(Object::toString);
        //
        pli.outClear();
        final var GO_BACK = -1;
        final StrictList<Tuple2<Integer, PmonMove>> movesOrdered = I.of(mon.moves).apply(strict(List()), (move, l) -> l.add(tuple(l.size()+1, move)));
        PmonDecision decision_ = null;
        do {
            fieldPrinter.accept();
            pli.outPrintAlignLeft("Choose move (or %s to go back)".formatted(GO_BACK));
            for (var moveOrdered: movesOrdered) {
                pli.outPrintAlignLeft("[%s] %s".formatted(moveOrdered.a1, moveNameGetter.apply(moveOrdered.a2.id)));
            }
            String moveRead = pli.getIn().nextLine();
            try {
                int moveIndex = Integer.parseInt(moveRead);
                if (moveIndex == GO_BACK) {
                    pli.outClear();
                    return null;
                }
                var useMove = new PmonDecisionToUseMove();
                useMove.moveIndex = moveIndex - 1;
                if (useMove.moveIndex < 0 || useMove.moveIndex >= movesOrdered.size()) {
                    pli.outClear();
                    pli.outPrintAlignLeft("Invalid index for move: %s".formatted(moveIndex));
                    continue;
                }
                useMove.target = PmonDecisionToUseMove.Target.mon(new MonPartyFieldPosition(context.foeParties.keySet().iterator().next(), context.foeParties.values().iterator().next().keySet().iterator().next())); //TODO: fix this - works only in case of foe 1 party with 1 mon
                decision_ = PmonDecision.from(useMove);
            } catch (Exception ex) {
                pli.outClear();
                pli.outPrintAlignLeft("Not a valid choice: %s".formatted(moveRead));
            }
        }
        while (decision_ == null);
        return decision_;
    }
}
