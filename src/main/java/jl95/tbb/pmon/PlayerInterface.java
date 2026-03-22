package jl95.tbb.pmon;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.Scanner;

import static jl95.lang.SuperPowers.method;

public class PlayerInterface {

    private static final int foeInfoLineAlignIndex = 32;

    private PrintStream outNullable;
    private InputStream inNullable;

    public PlayerInterface out(PrintStream out) {
        this.outNullable = out;
        return this;
    }
    public PlayerInterface in(InputStream in) {
        this.inNullable = in;
        return this;
    }

    public PrintStream getOut() {
        return Optional.ofNullable(outNullable).orElse(System.out);
    }
    public void outClear() {
        getOut().print("\033\143");
    }
    public void outPrintAlignLeft(Object printable) {
        getOut().printf("%s%n", printable.toString());
    }
    public void outPrintAlignRight(Object printable) {
        String info = printable.toString();
        getOut().printf("%s%s%n", " ".repeat(Math.max(foeInfoLineAlignIndex - info.length(), 0)), info);
    }
    public Scanner getIn() {
        return new Scanner(Optional.ofNullable(inNullable).orElse(System.in));
    };
}
