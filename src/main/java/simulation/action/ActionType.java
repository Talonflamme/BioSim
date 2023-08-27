package simulation.action;

import simulation.Simulation;

public enum ActionType {
        /**
         * Movement in x direction.
         */
        MoveX,
        /**
         * Movement in y direction.
         */
        MoveY,
        /**
         * Random movement in any direction
         */
        MoveRand,
        /**
         * Do Nothing
         */
        DoNothing,
        /**
         * Move in the direction of the last step
         */
        MoveForward,
        /**
         * Move in the opposite direction of last step
         */
        MoveReverse,
        /**
         * Kill the agent directly in front.
         */
        KillForward;

        public boolean isEnabled() {
            return switch(this) {
                case MoveX, MoveReverse, MoveForward, DoNothing, MoveRand, MoveY -> true;
                case KillForward -> Simulation.config().isKillEnabled();
            };
        }
    }
