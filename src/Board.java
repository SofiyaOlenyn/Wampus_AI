
import java.util.Random;
import java.io.IOException;

public class Board
{
	private int pMax=2;

	private Square[][]	board;
	private Random random;
	private class Square
	{
		boolean pit    = false;
		boolean wumpus = false;
		boolean glitter = false;
		boolean breeze = false;
		boolean stench = false;
	}


	private Agent 	agent;
	private Agent.Action	lastAction;

	private int 	score;
	private boolean	hasArrow,bump,scream;
	private int agentDirection;
	private int	agentX,agentY;

	private int colSize,rowSize;


public Board( )
{

		score        = 0;
		agentDirection = 0;
		agentX       = 0;
		agentY       = 0;
	hasArrow     = true;
	bump         = false;
	scream       = false;
		lastAction   = Agent.Action.CLIMB;
		agent = new AgentAI();

		{

			random = new Random ( );
			colSize = 4;
			rowSize = 4;
			board = new Square[colSize][rowSize];

			for (int r = 0; r < rowSize; ++r )
				for (int c = 0; c < colSize; ++c )
				{	board[c][r] = new Square();
			}

			fillBoard( );
		}
	}
	

	public int run ( )
	{
		while ( score >= -1000 )
		{
		printBoard();

			lastAction = agent.getAction
			(
				board[agentX][agentY].stench,
				board[agentX][agentY].breeze,
				board[agentX][agentY].glitter,
				bump,
				scream
			);

			--score;
			bump   = false;
			scream = false;
			
			switch ( lastAction )
			{
				case TURN_LEFT:
					if (--agentDirection < 0) agentDirection = 3;
					break;
					
				case TURN_RIGHT:
					if (++agentDirection > 3) agentDirection = 0;
					break;
					
				case FORWARD:
					if ( agentDirection == 0 && agentX+1 < colSize)
						++agentX;
					else if ( agentDirection == 1 && agentY-1 >= 0 )
						--agentY;
					else if ( agentDirection == 2 && agentX-1 >= 0 )
						--agentX;
					else if ( agentDirection == 3 && agentY+1 < rowSize)
						++agentY;
					else
						bump = true;
					
					if ( board[agentX][agentY].pit || board[agentX][agentY].wumpus )
					{
						score -= 1000;
						 printBoard();
						return score;
					}
					break;
				
				case SHOOT:
					if ( hasArrow )
					{
						hasArrow = false;
						score -= 10;
						if ( agentDirection == 0 )
						{
							for (int x = agentX; x < colSize; ++x )
								if ( board[x][agentY].wumpus )
								{
									board[x][agentY].wumpus = false;
									board[x][agentY].stench = true;
									scream = true;
								}
						}
						else if ( agentDirection == 1 )
						{
							for ( int y = agentY; y >= 0; --y )
								if ( board[agentX][y].wumpus )
								{
									board[agentX][y].wumpus = false;
									board[agentX][y].stench = true;
									scream = true;
								}
						}
						else if ( agentDirection == 2 )
						{
							for ( int x = agentX; x >= 0; --x )
								if ( board[x][agentY].wumpus )
								{
									board[x][agentY].wumpus = false;
									board[x][agentY].stench = true;
									scream = true;
								}
						}
						else if ( agentDirection == 3 )
						{
							for (int y = agentY; y < rowSize; ++y )
								if ( board[agentX][y].wumpus )
								{
									board[agentX][y].wumpus = false;
									board[agentX][y].stench = true;
									scream = true;
								}
						}
					}
					break;
					
				case GRAB:
					if ( board[agentX][agentY].glitter)
					{
						board[agentX][agentY].glitter = false;

						score+=1000;
					}
					break;
					
				case CLIMB:
					if ( agentX == 0 && agentY == 0 )
					{

						printBoard();
						return score;
					}
					break;
			}
		}
		return score;
	}

	private void fillBoard( )
	{

		int goldX = randomInt(colSize);
		int goldY = randomInt(rowSize);

		while ( goldX == 0 && goldY == 0 )
		{
			goldX = randomInt(colSize);
			goldY = randomInt(rowSize);
		}

		addGold ( goldX, goldY );



		int p=0;
		do {
			for (int r = 0; r < rowSize; ++r)
				for (int c = 0; c < colSize; ++c)
					if ((c != 0 || r != 0) && p < pMax && randomInt(10) < 2) {
						//if (goldX != c && goldY != r)
						{

							addPit(c, r);
							p++;
						}
					}
		}while(p==0);



		int wampusX = randomInt(colSize);
		int wampusY = randomInt(rowSize);

		while ( wampusX == 0 && wampusY == 0 )
		{
			wampusX = randomInt(colSize);
			wampusY = randomInt(rowSize);
		}

		addWumpus ( wampusX, wampusY );


	}

	
	private void addPit ( int c, int r )
	{
		if ( isInBounds(c, r) )
		{
			board[c][r].pit = true;
			addBreeze ( c+1, r );
			addBreeze ( c-1, r );
			addBreeze ( c, r+1 );
			addBreeze ( c, r-1 );
		}
	}
	
	private void addWumpus ( int c, int r )
	{
		if ( isInBounds(c, r) )
		{
			board[c][r].wumpus = true;
			addStench ( c+1, r );
			addStench ( c-1, r );
			addStench ( c, r+1 );
			addStench ( c, r-1 );
		}
	}
	
	private void addGold ( int c, int r )
	{
		if ( isInBounds(c, r) )
			board[c][r].glitter = true;
	}
	
	private void addStench ( int c, int r )
	{
		if ( isInBounds(c, r) )
			board[c][r].stench = true;
	}
	
	private void addBreeze ( int c, int r )
	{
		if ( isInBounds(c, r) )
			board[c][r].breeze = true;
	}
	
	private boolean isInBounds ( int c, int r )
	{
		return ( c < colSize && r < rowSize && c >= 0  && r >= 0);
	}
	

	private void printBoard( )
	{
		printBoardInfo();

		printAgentInfo();
	}
	
	private void printBoardInfo ( )
	{
		System.out.println("******************************************");

		for (int r = rowSize -1; r >= 0; --r )
		{
			System.out.print("*");
			//System.out.print("|");
			for (int c = 0; c < colSize; ++c ) {
				//System.out.println("|");
				printTileInfo(c, r);
			}
			System.out.print("*");
			System.out.println("");
		}

		System.out.println("******************************************");
	}

	private void printTileInfo ( int c, int r )
	{
		StringBuilder tileString = new StringBuilder();
		tileString.append("|");
		if (board[c][r].pit)    tileString.append("p");
		if (board[c][r].wumpus) tileString.append("w");
		if (board[c][r].glitter)   tileString.append("g");
		if (board[c][r].breeze) tileString.append("b");
		if (board[c][r].stench) tileString.append("s");
		
		if ( agentX == c && agentY == r )
			tileString.append("A");
		
		//tileString.append("");
		tileString.append("|");
		System.out.printf("%6s", tileString.toString());
	}
	
	private void printAgentInfo ( )
	{
		System.out.println("-Statistics-");
		switch (agentDirection)
		{
			case 0:
				System.out.println("Agent direction: right");
				break;

			case 1:
				System.out.println("Agent direction: down");
				break;

			case 2:
				System.out.println("Agent direction: left");
				break;

			case 3:
				System.out.println("Agent direction: up");
				break;

			default:
				System.out.println("Agent direction: Invalid");
		}

		switch (lastAction)
		{
			case TURN_LEFT:
				System.out.println("Last action: turned left");
				break;

			case TURN_RIGHT:
				System.out.println("Last action: turned right");
				break;

			case FORWARD:
				System.out.println("Last action: moved forward");
				break;

			case SHOOT:
				System.out.println("Last action: shoot the arrow");
				break;

			case GRAB:
				System.out.println("Last action: grabbed");
				break;

			case CLIMB:
				System.out.println("Last action: climbed");
				break;

			default:
				System.out.println("Last action: Invalid");
		}
		StringBuilder perceptString = new StringBuilder("Percepts: ");

		if (board[agentX][agentY].stench) perceptString.append("stench, ");
		if (board[agentX][agentY].breeze) perceptString.append("breeze, ");
		if (board[agentX][agentY].glitter)   perceptString.append("glitter, ");
		if (bump)                         perceptString.append("bump, ");
		if (scream)                       perceptString.append("scream");

		if ( perceptString.charAt(perceptString.length()-1) == ' '
				&& perceptString.charAt(perceptString.length()-2) == ',' )
		{
			perceptString.deleteCharAt(perceptString.length()-1);
			perceptString.deleteCharAt(perceptString.length()-1);
		}

		System.out.println(perceptString.toString());
		System.out.println("Score: "   + score);
	}
	


	private int randomInt ( int limit )
	{
		return random.nextInt(limit);
	}
}
