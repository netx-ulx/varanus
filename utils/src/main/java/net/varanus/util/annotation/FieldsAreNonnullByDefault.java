package net.varanus.util.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;


/**
 * This annotation can be applied to a package or class to indicate that the
 * class fields in that element are nonnull by default unless there is:
 * <ul>
 * <li>An explicit nullness annotation
 * <li>there is a default field annotation applied to a more tightly nested
 * element.
 * </ul>
 */
@Documented
@Nonnull
@TypeQualifierDefault( ElementType.FIELD )
@Retention( RetentionPolicy.RUNTIME )
public @interface FieldsAreNonnullByDefault
{
    //
}
