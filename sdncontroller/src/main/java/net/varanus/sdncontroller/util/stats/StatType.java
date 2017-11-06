package net.varanus.sdncontroller.util.stats;

/**
 * Type of monitoring statistic.
 */
public enum StatType
{
    /**
     * A statistic of this type can be trusted even in the presence of network
     * attacks that aim to disrupt the monitoring.
     */
    SAFE
    {
        @Override
        public String toString()
        {
            return "safe";
        }
    },

    /**
     * A statistic of this type cannot be trusted because it is susceptible of
     * being manipulated by an adversary that aims to disrupt the monitoring.
     */
    UNSAFE
    {
        @Override
        public String toString()
        {
            return "unsafe";
        }
    };
}
