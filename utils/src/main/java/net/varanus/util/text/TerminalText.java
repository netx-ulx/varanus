package net.varanus.util.text;


import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.BaseBuilder;


/**
 *
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TerminalText
{
    public static Builder newBuilder()
    {
        return new Builder();
    }

    private final String format;

    private TerminalText( String format )
    {
        this.format = format;
    }

    public String format( Object text )
    {
        return String.format(format, text);
    }

    public String getFormat()
    {
        return format;
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof TerminalText)
               && this.equals((TerminalText)other);
    }

    public boolean equals( TerminalText other )
    {
        return (other != null)
               && this.format.equals(other.format);
    }

    @Override
    public int hashCode()
    {
        return format.hashCode();
    }

    @Override
    public String toString()
    {
        // TODO may be improved to show the used parameters (colors, etc.)
        return format;
    }

    public static final class Builder implements BaseBuilder<TerminalText>
    {
        private static final String PREFIX    = "\033[";
        private static final char   SEPARATOR = ';';
        private static final String SUFFIX    = "m";

        private static final String RESET = PREFIX + '0' + SUFFIX;

        private @Nullable Intensity intensity;
        private final Set<Shape>    shapes;
        private @Nullable Color     textColor;
        private @Nullable Color     backColor;

        private Builder()
        {
            this.intensity = null;
            this.shapes = EnumSet.noneOf(Shape.class);
            this.textColor = null;
            this.backColor = null;
        }

        public Builder intensity( Intensity intensity )
        {
            this.intensity = Objects.requireNonNull(intensity);
            return this;
        }

        public Builder shape( Shape shape )
        {
            this.shapes.add(Objects.requireNonNull(shape));
            return this;
        }

        public Builder textColor( Color color )
        {
            this.textColor = Objects.requireNonNull(color);
            return this;
        }

        public Builder backgroundColor( Color color )
        {
            this.backColor = Objects.requireNonNull(color);
            return this;
        }

        public Builder clear()
        {
            this.intensity = null;
            this.shapes.clear();
            this.textColor = null;
            this.backColor = null;
            return this;
        }

        @Override
        public TerminalText build()
        {
            Intensity intensity = this.intensity;
            Shape[] shapes = this.shapes.toArray(new Shape[this.shapes.size()]);
            Color textColor = this.textColor;
            Color backColor = this.backColor;

            if (intensity == null && shapes.length == 0 && textColor == null && backColor == null) {
                // unformatted text
                return new TerminalText("%s");
            }
            else {
                boolean isFirst = true;
                StringBuilder fmt = new StringBuilder();
                fmt.append(PREFIX);
                if (intensity != null) {
                    isFirst = false;
                    fmt.append(SGRParam.codeFor(intensity));
                }
                for (Shape shape : shapes) {
                    if (isFirst)
                        isFirst = false;
                    else
                        fmt.append(SEPARATOR);

                    fmt.append(SGRParam.codeFor(shape));
                }
                if (textColor != null) {
                    if (isFirst)
                        isFirst = false;
                    else
                        fmt.append(SEPARATOR);

                    fmt.append(SGRParam.codeForText(textColor));
                }
                if (backColor != null) {
                    if (isFirst)
                        isFirst = false;
                    else
                        fmt.append(SEPARATOR);

                    fmt.append(SGRParam.codeForBackground(backColor));
                }
                fmt.append(SUFFIX);
                fmt.append("%s");
                fmt.append(RESET);
                return new TerminalText(fmt.toString());
            }
        }
    }

    public static enum Intensity
    {
        BOLD, FAINT;
    }

    public static enum Shape
    {
        ITALIC, UNDERLINE, CROSSED_OUT;
    }

    public static enum Color
    {
        BLACK, BRIGHT_BLACK,
        RED, BRIGHT_RED,
        GREEN, BRIGHT_GREEN,
        YELLOW, BRIGHT_YELLOW,
        BLUE, BRIGHT_BLUE,
        MAGENTA, BRIGHT_MAGENTA,
        CYAN, BRIGHT_CYAN,
        WHITE, BRIGHT_WHITE;
    }

    private static enum SGRParam
    {
        // Text intensity //
        BOLD(1), FAINT(2),

        // Text shape //
        ITALIC(3), UNDERLINE(4), CROSSED_OUT(9),

        // Text color //
        TXT_BLACK(30), TXT_BRIGHT_BLACK(90),
        TXT_RED(31), TXT_BRIGHT_RED(91),
        TXT_GREEN(32), TXT_BRIGHT_GREEN(92),
        TXT_YELLOW(33), TXT_BRIGHT_YELLOW(93),
        TXT_BLUE(34), TXT_BRIGHT_BLUE(94),
        TXT_MAGENTA(35), TXT_BRIGHT_MAGENTA(95),
        TXT_CYAN(36), TXT_BRIGHT_CYAN(96),
        TXT_WHITE(37), TXT_BRIGHT_WHITE(97),

        // Background color //
        BGR_BLACK(40), BGR_BRIGHT_BLACK(100),
        BGR_RED(41), BGR_BRIGHT_RED(101),
        BGR_GREEN(42), BGR_BRIGHT_GREEN(102),
        BGR_YELLOW(43), BGR_BRIGHT_YELLOW(103),
        BGR_BLUE(44), BGR_BRIGHT_BLUE(104),
        BGR_MAGENTA(45), BGR_BRIGHT_MAGENTA(105),
        BGR_CYAN(46), BGR_BRIGHT_CYAN(106),
        BGR_WHITE(47), BGR_BRIGHT_WHITE(107);

        static int codeFor( Intensity intensity )
        {
            switch (intensity) {
                case BOLD:
                    return BOLD.code;

                case FAINT:
                    return FAINT.code;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }

        static int codeFor( Shape shape )
        {
            switch (shape) {
                case ITALIC:
                    return ITALIC.code;

                case UNDERLINE:
                    return UNDERLINE.code;

                case CROSSED_OUT:
                    return CROSSED_OUT.code;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }

        static int codeForText( Color color )
        {
            switch (color) {
                case BLACK:
                    return TXT_BLACK.code;

                case BRIGHT_BLACK:
                    return TXT_BRIGHT_BLACK.code;

                case RED:
                    return TXT_RED.code;

                case BRIGHT_RED:
                    return TXT_BRIGHT_RED.code;

                case GREEN:
                    return TXT_GREEN.code;

                case BRIGHT_GREEN:
                    return TXT_BRIGHT_GREEN.code;

                case YELLOW:
                    return TXT_YELLOW.code;

                case BRIGHT_YELLOW:
                    return TXT_BRIGHT_YELLOW.code;

                case BLUE:
                    return TXT_BLUE.code;

                case BRIGHT_BLUE:
                    return TXT_BRIGHT_BLUE.code;

                case MAGENTA:
                    return TXT_MAGENTA.code;

                case BRIGHT_MAGENTA:
                    return TXT_BRIGHT_MAGENTA.code;

                case CYAN:
                    return TXT_CYAN.code;

                case BRIGHT_CYAN:
                    return TXT_BRIGHT_CYAN.code;

                case WHITE:
                    return TXT_WHITE.code;

                case BRIGHT_WHITE:
                    return TXT_BRIGHT_WHITE.code;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }

        static int codeForBackground( Color color )
        {
            switch (color) {
                case BLACK:
                    return BGR_BLACK.code;

                case BRIGHT_BLACK:
                    return BGR_BRIGHT_BLACK.code;

                case RED:
                    return BGR_RED.code;

                case BRIGHT_RED:
                    return BGR_BRIGHT_RED.code;

                case GREEN:
                    return BGR_GREEN.code;

                case BRIGHT_GREEN:
                    return BGR_BRIGHT_GREEN.code;

                case YELLOW:
                    return BGR_YELLOW.code;

                case BRIGHT_YELLOW:
                    return BGR_BRIGHT_YELLOW.code;

                case BLUE:
                    return BGR_BLUE.code;

                case BRIGHT_BLUE:
                    return BGR_BRIGHT_BLUE.code;

                case MAGENTA:
                    return BGR_MAGENTA.code;

                case BRIGHT_MAGENTA:
                    return BGR_BRIGHT_MAGENTA.code;

                case CYAN:
                    return BGR_CYAN.code;

                case BRIGHT_CYAN:
                    return BGR_BRIGHT_CYAN.code;

                case WHITE:
                    return BGR_WHITE.code;

                case BRIGHT_WHITE:
                    return BGR_BRIGHT_WHITE.code;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }

        private final int code;

        private SGRParam( int code )
        {
            if (code < 0) throw new IllegalArgumentException("code number must be non-negative");
            this.code = code;
        }
    }
}
