

abstract class Agent
{
	public enum Action
	{
		TURN_LEFT,
		TURN_RIGHT,
		FORWARD,
		SHOOT,
		GRAB,
		CLIMB
	}

	public abstract Action getAction
	(
		boolean stench,
		boolean breeze,
		boolean glitter,
		boolean bump,
		boolean scream
	);
	
}
