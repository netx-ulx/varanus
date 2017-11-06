package net.varanus.util.openflow.types;

/**
 * 
 */
public enum MaskType
{
    NONE
    {
        @Override
        public boolean isExact()
        {
            return true;
        }

        @Override
        public boolean isPartiallyMasked()
        {
            return false;
        }

        @Override
        public boolean isWildcard()
        {
            return false;
        }
    },

    PARTIAL
    {
        @Override
        public boolean isExact()
        {
            return false;
        }

        @Override
        public boolean isPartiallyMasked()
        {
            return true;
        }

        @Override
        public boolean isWildcard()
        {
            return false;
        }
    },

    FULL
    {
        @Override
        public boolean isExact()
        {
            return false;
        }

        @Override
        public boolean isPartiallyMasked()
        {
            return false;
        }

        @Override
        public boolean isWildcard()
        {
            return true;
        }
    };

    public abstract boolean isExact();

    public abstract boolean isPartiallyMasked();

    public abstract boolean isWildcard();
}
