package net.varanus.util.openflow.types;


import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <FD>
 *            The flowable directable type
 * @param <F>
 *            The flowable type
 * @param <D>
 *            The directable type
 */
//@formatter:off
@ReturnValuesAreNonnullByDefault
public interface FlowedDirected<FD extends Flowable<FD> & Directable<FD>,
                                F extends Flowable<F> & Directed<FD>,
                                D extends Directable<D> & Flowed<FD>> extends Flowed<F>, Directed<D>
{//@formatter:on
    public FD unflowDirected();
}
