package net.varanus.util.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;


/**
 * <b>
 * NOTE: Copied verbatim from
 * {@code edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault} but
 * without the {@code Deprecated} annotation since there is not an equivalent
 * annotation in the {@code javax.annotation} package at the time this was
 * written.
 * </b>
 * <p>
 * This annotation can be applied to a package, class or method to indicate that
 * the methods in that element have nonnull return values by default unless
 * there is:
 * <ul>
 * <li>An explicit nullness annotation
 * <li>The method overrides a method in a superclass (in which case the
 * annotation of the corresponding parameter in the superclass applies)
 * <li>there is a default annotation applied to a more tightly nested element.
 * </ul>
 */
@Documented
@Nonnull
@TypeQualifierDefault( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface ReturnValuesAreNonnullByDefault
{
    //
}
