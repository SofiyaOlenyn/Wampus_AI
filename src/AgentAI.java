

import java.util.*;

public class AgentAI extends Agent
{

    Node initialState, nextGoal,wumpus;
    Stack<Node> DFSqueue,pathToOrigin;

    private enum Direction {  RIGHT, DOWN, LEFT, UP;}
    private enum Location {LEFT_UP,  UP, RIGHT_UP, LEFT, MIDDLE, RIGHT, LEFT_DOWN, DOWN, RIGHT_DOWN;}

    private class Neighbor
    {
        int xN = 0; int yN = 0;
        boolean valid = false;
    }

    private class Square
    {
        int visitedNumber = 0;
        boolean breeze = false;
        boolean stench = false;
        double pitProbability = 0.2;
        double wumpusProbability = 1.0/((double) maxSize *(double) maxSize);
        boolean queue = false;
    }



    //збереження подальших дій
    LinkedList<Agent.Action> orderOfActions;


    final int maxSize = 7;
    private boolean goldGrabed;
    private boolean goldImpossible; // золото в ямі або оточена ямами
    private boolean	hasArrow, wumpusIsKnown,wumpusAlive, killWumpus;    // тільки тоді коли схоже на те, що золото там, де вампус
    private Direction direction;	// напрямки : 0 - праворуч, 1 - вниз, 2 - ліво, 3 - вгору
    private boolean moving;     // агент рухаться до наступної цілі
    private int		agentX, agentY;
    private Direction lastDirection;

    private int		lastAgentX,lastAgentY;
    private Agent.Action	lastAction;
    private Square[][]	board;
    private Neighbor[] neighbors;

    private int actionCount;

    private boolean initialPath;
    //кількість досліджена агентом
    private int colSizeLogic,rowSizeLogic;




    public AgentAI( )
    {

        goldGrabed = false;
        goldImpossible = false;
        hasArrow     = true;
        wumpusAlive = true;
        wumpusIsKnown = false;
        killWumpus   = false;
        direction = Direction.RIGHT;
        moving       = false;
        agentX       = 0;
        agentY       = 0;
        lastDirection = Direction.RIGHT;
        lastAgentX  = -1;
        lastAgentY  = -1;
        lastAction   = Action.CLIMB;
        board = new Square[maxSize][maxSize];

        for (int r = 0; r < maxSize; ++r )
            for (int c = 0; c < maxSize; ++c )
                board[c][r] = new Square();


        neighbors = new Neighbor[4]; // відносний напрямок
        for ( int i = 0; i < 4; i++)
            neighbors[i] = new Neighbor();
        neighbors[0].xN = 1;
        neighbors[1].yN = -1;
        neighbors[2].xN = -1;
        neighbors[3].yN = 1;

        colSizeLogic = maxSize;
        rowSizeLogic = maxSize;

        orderOfActions = new LinkedList<Agent.Action>();

        actionCount = 0;

        initialState = new Node(0,0);
        nextGoal = new Node(0,0);
        wumpus = new Node(colSizeLogic - 1, rowSizeLogic - 1);
        DFSqueue = new Stack<Node>();
        pathToOrigin = new Stack<Node>();

        initialPath = false;

    }

    //припустимо, вампус і яма незалежні, тому P (безпечно) = P (без вампус) * P (без ями)

    private double safeProbability(int x, int y){

        if(x >= colSizeLogic || y >= rowSizeLogic) return 0.0;
        return (1 - board[x][y].wumpusProbability)*(1 - board[x][y].pitProbability);
    }

    private void setWampusProbability(int x, int y, double p){

        if(board[x][y].wumpusProbability != 0){ //якщо воно дорівнює нулю, це означає, що ми впевнені, що ями немає, тому не потрібно міняти
            board[x][y].wumpusProbability = p;
        }
    }


    private void setPitProbability(int x, int y, double p){

        if(board[x][y].pitProbability != 0)
            board[x][y].pitProbability = p;

    }

    @Override
    public Action getAction
            (
                    boolean stench,
                    boolean breeze,
                    boolean glitter,
                    boolean bump,
                    boolean scream
            )
    {


        //завершити попередню послідовність дій

        actionCount++;

        if (!orderOfActions.isEmpty()) {

            lastAction = orderOfActions.poll();
            return lastAction;
        }

        //оновити модель на останові нових відчуттів

        if(bump && lastAction == Action.FORWARD){
            //треба обернутися
            switch(direction){

                case RIGHT:
                    agentX = agentX - 1;
                    colSizeLogic = agentX + 1;
                    break;

                case UP:
                    agentY = agentY - 1;
                    rowSizeLogic = agentY + 1;
                    break;

                case DOWN:

                    break;

                case LEFT:

                    break;


            }

            // оскільки поточна мета недійсна (через межу), потрібно повторно встановити ціль

            setNextAim();
            moving = true;
            return moveToAim();
        }

        if(lastAction == Action.SHOOT){

            if(scream){

                wumpusAlive = false;
                setWumpusProbability(agentX, agentY); //коли вампус вмирає Prob(wumpus) = 0
            }
            else{ //якщо ми знаємо що попереду не вампус то настпуна клітинка Prob(wumpus) = 0

                wumpusAlive = true;

                switch(direction){

                    case RIGHT:
                        for(int i = agentX; i < colSizeLogic; i++)
                            board[i][agentY].wumpusProbability = 0;
                        break;

                    case LEFT:
                        for(int i = agentX; i >=0; i--)
                            board[i][agentY].wumpusProbability = 0;
                        break;

                    case UP:
                        for(int i = agentY; i < rowSizeLogic; i++)
                            board[agentX][i].wumpusProbability = 0;
                        break;

                    case DOWN:
                        for(int i = agentY; i >= 0; i--)
                            board[agentX][i].wumpusProbability = 0;
                        break;
                }

                updateWumpusProbability(agentX, agentY); //спроба догадатися де вампус
            }
        }

        if(lastAction == Action.CLIMB || (lastAction == Action.FORWARD && !bump)){

        // відстежуємо вихідний шлях на випадок, якщо агенту потрібно пройти його, щоб повернутися назад

             if(!initialPath){

                if(!pathToOrigin.isEmpty()){

                    Node node_1 = pathToOrigin.peek();

                    if(agentX == node_1.parentX && agentY == node_1.parentY){

                        pathToOrigin.pop();
                    }
                    else{

                        Node node = new Node(agentX, agentY);
                        node.parentX = lastAgentX;
                        node.parentY = lastAgentY;
                        pathToOrigin.push(node);
                    }
                }
                else{

                    Node node = new Node(agentX, agentY);
                    node.parentX = lastAgentX;
                    node.parentY = lastAgentY;
                    pathToOrigin.push(node);
                }
            }


            board[agentX][agentY].visitedNumber++;

            //якщо ми ще не булт в цій клітинці
            if(board[agentX][agentY].visitedNumber == 1){

                //додати нові сприйняття
                board[agentX][agentY].breeze = breeze;
                board[agentX][agentY].stench = stench;



                board[agentX][agentY].pitProbability = 0;
                board[agentX][agentY].wumpusProbability = 0;

                generateNeighbors(agentX, agentY);

                if(!breeze){

                    for(int i = 0; i < 4; i++){

                        if(neighbors[i].valid){

                            int x_1, y_1;
                            x_1 = agentX + neighbors[i].xN;
                            y_1 = agentY + neighbors[i].yN;

                            board[x_1][y_1].pitProbability = 0;
                        }
                    }
                }
                else{

                    for(int i = 0; i < 4; i++){

                        if(neighbors[i].valid){

                            int x_1, y_1;
                            x_1 = agentX + neighbors[i].xN;
                            y_1 = agentY + neighbors[i].yN;

                            setPitProbability(x_1, y_1, 1.0);
                        }
                    }
                }

                //якщо вампус живий і невідомо де він то робимо висновки про шум і сморід = 0

                if(wumpusAlive && !wumpusIsKnown){

                    if(!stench){

                        for(int i = 0; i < 4; i++){

                            if(neighbors[i].valid){

                                int x_1, y_1;
                                x_1 = agentX + neighbors[i].xN;
                                y_1 = agentY + neighbors[i].yN;

                                board[x_1][y_1].wumpusProbability = 0;
                            }
                        }
                    }
                } //if(wumpusAlive && !wumpusIsKnown)
            } //if visitedNumber == 1
        }

     //updateWumpusProbability по новій інфі
        if(stench && wumpusAlive && !wumpusIsKnown) updateWumpusProbability(agentX, agentY);

        // розширюємо поточний вузол, щоб генерувати дочірні об'єкти та поміщаємо їх у стек
        expandNode(agentX, agentY);

        //беремо золото
        if(!goldGrabed && glitter){


            moving = false;
            goldGrabed = true;
            goldImpossible = false;

            lastAction   = Action.GRAB;
            return Action.GRAB;
        }

        if(agentX == 0 && agentY == 0){

//  вилазити, якщо на початковому вузлі вітер, бо те що яма ймовірно
            if(breeze || goldGrabed || goldImpossible) {

                lastAction = Action.CLIMB;
                return Action.CLIMB;
            }
        }

        //якщо невідомо де вампус можна вистрілити ризикнувши щоб перевірити де він
        if(stench && wumpusAlive && !wumpusIsKnown && hasArrow && usefulShoot(direction.ordinal())) {

            hasArrow = false;
            lastAction = Action.SHOOT;
            return Action.SHOOT;
        }

        if(stench && wumpusAlive && killWumpus && hasArrow){

            generateNeighbors(agentX, agentY);

            int i;

            for(i = 0; i < 4; i++){

                if(neighbors[i].valid){

                    int x_1, y_1;
                    x_1 = agentX + neighbors[i].xN;
                    y_1 = agentY + neighbors[i].yN;


                    if(x_1 == wumpus.x && y_1 == wumpus.y){

                        moving = false;
                        break;
                    }
                }
            }

            if(i < 4){


                return beginKillingWumpus(direction, i);
            }
        }

        //3 що робити далі
        // встановлення порогового значення стоп-лосса в 6 разів більше квадратів виявляється оптимальним

        if(!goldGrabed && !goldImpossible && actionCount > 2*3* colSizeLogic * rowSizeLogic){

            moving = false;
            goldImpossible = true;
        }

        if(moving){

            if(agentX == nextGoal.x && agentY == nextGoal.y){ //ціль досягнена

                moving = false;
            }
            else{

                return moveToAim();
            }
        }

      //якщо агент знайшов золото або вирішив здаватися то йдемо на початок
        if(goldGrabed || goldImpossible){

            nextGoal = initialState;
        }
        else{


            setNextAim();
        }

        moving = true;
        return moveToAim();

    }



    private void updateWumpusProbability(int x, int y){

        generateNeighbors(x, y);

        int w_count = 0;

        for(int i = 0; i < 4; i++){

            if(neighbors[i].valid){

                int x_1, y_1;

                x_1 = x + neighbors[i].xN;
                y_1 = y + neighbors[i].yN;

                if(board[x_1][y_1].wumpusProbability != 0){

                    setWampusProbability(x_1, y_1, 1.0);
                    wumpus.x = x_1;
                    wumpus.y = y_1;
                    w_count++;
                }
            }
        }

        if(w_count == 1){ //напевне один квадрат і це вампус

            setWumpusProbability(wumpus.x, wumpus.y);
        }
    }

    //оновити ймовірність , відемо де вампус
    private void setWumpusProbability(int x, int y){

        wumpusIsKnown = true;


        for(int i = 0; i < colSizeLogic; i++){

            for(int j = 0; j < rowSizeLogic; j++){

                board[i][j].wumpusProbability = 0;
            }
        }

        if(wumpusAlive){

            board[x][y].wumpusProbability = 1;
        }
    }

    //класифікація окацій агента


 //класифікує локції
    private Location locationCase(int x,int y){

        if(x == 0){

            if(y == 0) return Location.LEFT_DOWN;
            if(y == rowSizeLogic - 1) return Location.LEFT_UP;
            return Location.LEFT;
        }

        if(x == colSizeLogic - 1){

            if(y == 0) return Location.RIGHT_DOWN;
            if(y == rowSizeLogic - 1) return Location.RIGHT_UP;
            return Location.RIGHT;
        }

        if(y == 0) return Location.DOWN;
        if(y == rowSizeLogic - 1) return Location.UP;
        return Location.MIDDLE;
    }



    //генерує дітейвузла і добавляєїх у стек
    private void expandNode(int x, int y){

        generateNeighbors(x,y);

        //спочатку додаємо дітей, щоб їх розширили в останню чергу


        int backChild;
        backChild = direction.ordinal() + 2;
        if(backChild >= 4) backChild = backChild - 4;

        expandNodeHelper(x, y, backChild);

        for(int i = 0; i < 4; i++){

            if(i != direction.ordinal() && i != backChild){

                expandNodeHelper(x, y, i);
            }
        }

        //додаємо передніх дітей в останню чергу, щоб розшири першими
        expandNodeHelper(x, y, direction.ordinal());
    }
    //генерує Neighbor які базуються на позиції агента
    private void generateNeighbors(int x, int y){

        for(int i = 0; i < 4; i++) neighbors[i].valid = false;

        Location location = locationCase(x,y);

        switch(location){

            case LEFT_DOWN:
                neighbors[0].valid = true;
                neighbors[3].valid = true;
                break;

            case LEFT:
                neighbors[0].valid = true;
                neighbors[1].valid = true;
                neighbors[3].valid = true;
                break;

            case LEFT_UP:
                neighbors[0].valid = true;
                neighbors[1].valid = true;
                break;

            case DOWN:
                neighbors[0].valid = true;
                neighbors[2].valid = true;
                neighbors[3].valid = true;
                break;

            case MIDDLE:
                neighbors[0].valid = true;
                neighbors[1].valid = true;
                neighbors[2].valid = true;
                neighbors[3].valid = true;
                break;

            case UP:
                neighbors[0].valid = true;
                neighbors[1].valid = true;
                neighbors[2].valid = true;
                break;

            case RIGHT_DOWN:
                neighbors[2].valid = true;
                neighbors[3].valid = true;
                break;

            case RIGHT:
                neighbors[1].valid = true;
                neighbors[2].valid = true;
                neighbors[3].valid = true;
                break;

            case RIGHT_UP:
                neighbors[1].valid = true;
                neighbors[2].valid = true;
                break;
        }
    }
    private void expandNodeHelper(int x, int y, int i){

        if(neighbors[i].valid){

            int x_1, y_1;

            x_1 = x + neighbors[i].xN;
            y_1 = y + neighbors[i].yN;

            // додаємо лише 100% безпечні та невивчені вузли; не додавати вузол, якщо він був у черзі
            if(safeProbability(x_1, y_1) == 1 && board[x_1][y_1].visitedNumber == 0 && !board[x_1][y_1].queue){

                Node node = new Node(x_1, y_1);
                node.parentX = x;
                node.parentY = y;

                if(DFSqueue.search(node) == -1){

                    DFSqueue.push(node);
                    board[x_1][y_1].queue = true;
                }
            }
        }
    }

//перевіряємо чи варто стріляти, бо якщо у ямі, то немає сенсу
    private boolean usefulShoot(int dir){

        boolean useful = false;

        switch(dir){

            case 0:
                if(colSizeLogic - 1 > agentX){

                    if(board[agentX+1][agentY].pitProbability == 0) useful = true;
                }
                break;

            case 1:
                if(0 < agentY){

                    if(board[agentX][agentY-1].pitProbability == 0) useful = true;
                }
                break;

            case 2:
                if(0 < agentX){

                    if(board[agentX-1][agentY].pitProbability == 0) useful = true;
                }
                break;

            case 3:
                if(rowSizeLogic - 1 > agentY){

                    if(board[agentX][agentY+1].pitProbability == 0) useful = true;
                }
                break;
        }

        return useful;
    }

    //встановити вершину стеку ціллю
    private void setNextAim(){

        do{
            // якщо черга стає порожньою, це означає, що немає 100% безпечного та невивченого вузла
            // дуже ймовірно, що золото знаходиться в ямі або оточене ямами => здавайся!

            if(DFSqueue.empty()){

                //можливо золото під вампусом і треба його вбити
                if(!killWumpus && wumpusAlive && wumpusIsKnown && hasArrow && board[wumpus.x][wumpus.y].pitProbability == 0){

                    killWumpus = true;
                    nextGoal = wumpus;
                }
                else{

                    goldImpossible = true;
                    nextGoal = initialState;
                }
            }
            else{

                nextGoal = DFSqueue.pop();
            }

        }while(!(nextGoal.x < colSizeLogic && nextGoal.y < rowSizeLogic));
        // Можливо, додаючи вузол до черги, ми все ще не знаємо межі
        // тому перед розширенням потрібно перевірити, чи не перебуває він у межах
    }

    // вирішити, як рухатися до наступної мети
    // рандомізуємо методи, щоб уникнути залипання
    private Action moveToAim(){

        generateNeighbors(agentX, agentY);

        if(initialPath){

            return goInitialPath();
        }

        int validCount = 0;
        LinkedList<Integer> moveMethods = new LinkedList<Integer>();
        LinkedList<Integer> moveMethods_1 = new LinkedList<Integer>();

        for(int i = 0; i < 4; i++){

            if(neighbors[i].valid) {

                validCount++;
                moveMethods.addLast(i);
                moveMethods_1.addLast(i);
            }
        }

        double randomSelector;
        int selector, method, counter;

        counter = validCount;

        //спочатку вибераємо можливий найкоротший шлях до мети
        while(!moveMethods.isEmpty() && counter!=0){

            randomSelector = Math.random(); //between 0 and 1
            selector = (int)(randomSelector/(1.0/counter));

            method = moveMethods.get(selector);
            moveMethods.remove(selector);
            counter--;

            switch(method){

                case 0:
                    if(nextGoal.x > agentX && safeProbability(agentX+1, agentY) == 1)
                        return addActionOrder(direction, Direction.RIGHT);
                    break;

                case 1:
                    if(nextGoal.y < agentY && safeProbability(agentX, agentY-1) == 1)
                        return addActionOrder(direction, Direction.DOWN);
                    break;

                case 2:
                    if(nextGoal.x < agentX && safeProbability(agentX-1, agentY) == 1)
                        return addActionOrder(direction, Direction.LEFT);
                    break;

                case 3:
                    if(nextGoal.y > agentY && safeProbability(agentX, agentY+1) == 1)
                        return addActionOrder(direction, Direction.UP);
                    break;
            }
        }
// якщо шлях до початку руху обхідний, краще слідувати оригінальному шляху, щоб не застрягти
          if((goldGrabed || goldImpossible) && !initialPath){

            initialPath = true;
            pathToOrigin.pop(); // поп поточне місце розташування, перейти до попереднього місця розташування
            return goInitialPath();
        }

        //інакше рандомно ибираємо шлях до цілі
        counter = validCount;

        while(!moveMethods_1.isEmpty() && counter!=0){

            randomSelector = Math.random(); //between 0 and 1
            selector = (int)(randomSelector/(1.0/counter));

            method = moveMethods_1.get(selector);
            moveMethods_1.remove(selector);
            counter--;

            switch(method){

                case 0:
                    if(safeProbability(agentX+1, agentY) == 1)
                        return addActionOrder(direction, Direction.RIGHT);
                    break;

                case 1:
                    if(safeProbability(agentX, agentY-1) == 1)
                        return addActionOrder(direction, Direction.DOWN);
                    break;

                case 2:
                    if(safeProbability(agentX-1, agentY) == 1)
                        return addActionOrder(direction, Direction.LEFT);
                    break;

                case 3:
                    if(safeProbability(agentX, agentY+1) == 1)
                        return addActionOrder(direction, Direction.UP);
                    break;
            }
        }

        //GRAB не впливає
        lastAction = Action.GRAB;
        return Action.GRAB;
    }

    //якщо шлях до початку координат не є простим, краще дотримуйтесь початкового шляху, щоб не застрягти
    private Action goInitialPath(){

        Node node = pathToOrigin.pop();

        for(int i = 0; i < 4; i++){

            if(neighbors[i].valid){

                int x_1, y_1;

                x_1 = agentX + neighbors[i].xN;
                y_1 = agentY + neighbors[i].yN;

                if(x_1 == node.x && y_1 == node.y){

                    switch(i) {

                        case 0:
                            return addActionOrder(direction, Direction.RIGHT);

                        case 1:
                            return addActionOrder(direction, Direction.DOWN);

                        case 2:
                            return addActionOrder(direction, Direction.LEFT);

                        case 3:
                            return addActionOrder(direction, Direction.UP);
                    }
                }
            }
        }

        lastAction = Action.GRAB;
        return Action.GRAB;
    }

    // На основі поточного напрямку та бажаного напрямку додаємо послідовність дій до черги
    private Agent.Action addActionOrder(Direction currentDirection, Direction desiredDirection) {

        // резервне копіювання старого місця розташування
        lastAgentX = agentX;
        lastAgentY = agentY;
        lastDirection = direction;

        //апдейт
        agentX = agentX + neighbors[desiredDirection.ordinal()].xN;
        agentY = agentY + neighbors[desiredDirection.ordinal()].yN;
        direction = desiredDirection;

        lastAction = Action.FORWARD;

        switch(currentDirection) {

            case RIGHT:
                switch(desiredDirection) {
                    case RIGHT: // Facing R, go R: {F}
                        return Action.FORWARD;

                    case DOWN: // Facing R, go D: {R -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case LEFT: // Facing R, go L: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing R, go U: {L -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case DOWN:
                switch(desiredDirection) {
                    case RIGHT: // Facing D, go R: {L -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing D, go D: {F}
                        return Action.FORWARD;

                    case LEFT: // Facing D, go L: {R -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case UP: // Facing D, go U: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case LEFT:
                switch(desiredDirection) {
                    case RIGHT: // Facing L, go R: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing L, go D: {L -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing L, go L: {F}
                        return Action.FORWARD;

                    case UP: // Facing L, go U: {R -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;
                }

            case UP:
                switch(desiredDirection) {
                    case RIGHT: // Facing U, go R: {R -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case DOWN: // Facing U, go D: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing U, go L: {L -> F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing U, go U: {F}
                        return Action.FORWARD;
                }
        }


        agentX = lastAgentX;
        agentY = lastAgentY;
        direction = lastDirection;
//за замовчуванням бо не вплине ні на що
        lastAction = Action.GRAB;
        return Action.GRAB;
    }

    private Agent.Action beginKillingWumpus(Direction currentDirection, int aimD) {


        lastAgentX = agentX;
        lastAgentY = agentY;
        lastDirection = direction;


        agentX = agentX + neighbors[aimD].xN;
        agentY = agentY + neighbors[aimD].yN;

        switch(aimD) {

            case 0:
                direction = Direction.RIGHT;
                break;

            case 1:
                direction = Direction.DOWN;
                break;

            case 2:
                direction = Direction.LEFT;
                break;

            case 3:
                direction = Direction.UP;
                break;
        }

        hasArrow = false;
        wumpusAlive = false;
        setWumpusProbability(agentX, agentY); //коли вампус вмирає, усі квадрати мають можливість вампуса = 0
        lastAction = Action.FORWARD;

        switch(currentDirection) {

            case RIGHT:
                switch(direction) {
                    case RIGHT: // Facing R, go R: {F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.SHOOT;

                    case DOWN: // Facing R, go D: {R -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case LEFT: // Facing R, go L: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing R, go U: {L -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case DOWN:
                switch(direction) {
                    case RIGHT: // Facing D, go R: {L -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing D, go D: {F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.SHOOT;

                    case LEFT: // Facing D, go L: {R -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case UP: // Facing D, go U: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case LEFT:
                switch(direction) {
                    case RIGHT: // Facing L, go R: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing L, go D: {L -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing L, go L: {F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.SHOOT;

                    case UP: // Facing L, go U: {R -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;
                }

            case UP:
                switch(direction) {
                    case RIGHT: // Facing U, go R: {R -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case DOWN: // Facing U, go D: {L -> L -> F}
                        orderOfActions.addLast(Action.TURN_LEFT);
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing U, go L: {L -> F}
                        orderOfActions.addLast(Action.SHOOT);
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing U, go U: {F}
                        orderOfActions.addLast(Action.FORWARD);
                        return Action.SHOOT;
                }
        }

        agentX = lastAgentX;
        agentY = lastAgentY;
        direction = lastDirection;

        lastAction = Action.SHOOT;
        return Action.SHOOT;
    }


}