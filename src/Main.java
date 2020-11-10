
public class Main
{
    public static void main ( String[] args )
	{
			try
			{
				System.out.println("=======START WAMPUS GAME  ======");
				Board board = new Board();
				int score = board.run();
				System.out.println("=============== Score: " + score+" ==============");
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}


	}
}