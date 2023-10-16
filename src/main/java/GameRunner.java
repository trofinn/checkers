import com.google.gson.Gson;

import java.util.*;

/**
 * Responsible for determining the gamemode (1- or 2-player), running the game, and handling game exit.
 *
 * @author Mckenna Cisler
 * @version 11.23.2015
 */
public class GameRunner
{
    // game constants
    public static final int SIZE = 9;

    // define globally used variables
    private static Scanner input = new Scanner(System.in);
    private static boolean isPlayer1 = true;

    // define an easily accesible "end" variable
    private static boolean endGameNow = false;

    public static void main(String[] args)
    {
        // generate basic board and setup
        Board board = new Board(SIZE);

        JsonObject jsonObject = new JsonObject();
        // define abstract classes, to be assigned a concrete class after deciding gamemode
        HumanPlayer player1 = new HumanPlayer(true);
        HumanPlayer player2 = new HumanPlayer(false);
        clearScreen();
        String raw = input.nextLine().toLowerCase();
        if(raw.trim().equals("{\"init\":{\"players\":2}}")) {
            while ( !endGame(board) )
            {

                if (isPlayer1)
                {
                    jsonObject.requested_actions = new ArrayList<>();
                    RequestedAction requestedAction = new RequestedAction("CLICK", "1");
                    jsonObject.requested_actions.add(requestedAction);
                    ArrayList<Piece> showAllPieces = new ArrayList<>();
                    ArrayList<Piece> nextActionForBlack = new ArrayList<>();

                    for(Piece[] piece: board.boardArray) {
                        for (Piece p: piece
                        ) {
                            if(p != null ) {
                                showAllPieces.add(p);
                                if(!p.isWhite) {
                                    nextActionForBlack.add(p);

                                }
                                else {
                                    jsonObject.requested_actions.get(0).addZone(new Zone(p.x * 50, p.y * 50, 50, 50));
                                }
                            }
                        }
                    }
                    board = player1.getMove(board, jsonObject, "1");
                }
                else
                {
                    jsonObject.requested_actions = new ArrayList<>();
                    RequestedAction requestedAction = new RequestedAction("CLICK", "2");
                    jsonObject.requested_actions.add(requestedAction);
                    ArrayList<Piece> showAllPieces = new ArrayList<>();
                    ArrayList<Piece> nextActionForWhite = new ArrayList<>();

                    for(Piece[] piece: board.boardArray) {
                        for (Piece p: piece
                        ) {
                            if(p != null ) {
                                showAllPieces.add(p);
                                if(p.isWhite) {
                                    nextActionForWhite.add(p);
                                }
                                else {
                                    jsonObject.requested_actions.get(0).addZone(new Zone(p.x * 50, p.y * 50, 50, 50));
                                }
                            }
                        }
                    }
                    board = player2.getMove(board, jsonObject, "2");
                }

                // switch players and flip board for next player
                isPlayer1 = !isPlayer1;
            }
        }

    }

    /**
     * Queries the user to determine the requested gamemode
     * @return Returns true if the user wants two-player mode,
     * else false if they want one-player mode.
     */

    /**
     * Determines whether the game has been completed, or is in a stalemate
     * @param board The board to check to determine if we're at an endgame point.
     */
    private static boolean endGame(Board board)
    {
        // have an emergency trigger for endgame
        if (endGameNow)
            return true;
        else
        {
            // otherwise search the board for pieces of both colors, and if none of one color are present,
            // the other player has won.
            int movableWhiteNum = 0;
            int movableBlackNum = 0;
            for (int pos = 0; pos < board.size*board.size; pos++)
            {
                // make sure the piece exists, and if so sum movable pieces for each color)
                Piece pieceHere = board.getValueAt(pos);
                if (pieceHere != null)
                {
                    // only consider piece if it has possible moves
                    Move[] movesHere = pieceHere.getAllPossibleMoves(board);
                    if (movesHere != null && movesHere.length > 0)
                    {
                        if (pieceHere.isWhite)
                            movableWhiteNum++;
                        else if (!pieceHere.isWhite)
                            movableBlackNum++;
                    }
                }
            }

            // determine if anyone won (or if no one had any moves left)
            if (movableWhiteNum + movableBlackNum == 0)
                System.out.println("The game was a stalemate...");
            else if (movableWhiteNum == 0)
                System.out.println("Congratulations, Black, you have won the game gloriously!");
            else if (movableBlackNum == 0)
                System.out.println("Congratulations, White, you have won the game gloriously!");
            else
                return false;

            // we can only make it here if any of the above conditions are hit
            return true;
        }
    }

    /**
     * Responsible for quickly ending the game
     */
    public static void endGameNow()
    {
        endGameNow = true;
    }

    /**
     * Clears the terminal screen
     */
    public static void clearScreen()
    {
        // see http://stackoverflow.com/a/32008479/3155372
        System.out.print("\033[2J\033[1;1H");
    }
}




class Board
{
    // global vars
    public Piece[][] boardArray;
    public int size;

    /**
     * Responsible for generating a brand new board
     * @param size The size of the board (8 for common checkers)
     * NOTE: currently will probably break with other than 8 as size!
     */
    public Board(int size)
    {
        // new board is just empty
        this.boardArray = new Piece[size][size];

        // store the size for further use
        this.size = size;

        // setup the starting positions
        setupBoard();
    }

    /**
     * Responsible for generating a board based on another board
     */
    public Board(Board board)
    {
        // just transfer stuff
        this.boardArray = board.boardArray;
        this.size = board.size;
    }

    /**
     * Fills the board with pieces in their starting positions.
     * Adds WHITE pieces at the top to start (so white should move first)
     */
    public void setupBoard()
    {
        for (int y = 0; y < size; y++)
        {
            for (int x = 0; x < size; x++)
            {
                // add white pieces to the top (in a checkerboard pattern of black spaces - not on white spaces)
                if (y < 3 && isCheckerboardSpace(x, y))
                {
                    this.boardArray[y][x] = new Piece(x, y, true);
                }
                // ... and black pieces to the bottom in the opposite pattern
                else if (y >= size - 3 && isCheckerboardSpace(x, y))
                {
                    this.boardArray[y][x] = new Piece(x, y, false);
                }
            }
        }
    }

    /**
     * Using the given move and piece, move the piece on the board and apply it to this board.
     * @param move The Move object to execute on the piece and board.
     * @param piece The Piece object that will be moved.
     */
    public void applyMoveToBoard(Move move, Piece piece)
    {
        // NOTE: at this point, the starting position of the move (move.getStartingPosition) will not neccesarily
        // be equal to the piece's location, because jumping moves have no understanding of the root move
        // and therefore can only think back one jump. WE ARE PRESUMING that the piece given to this function
        // is the one which the move SHOULD be applied to, but due to this issue we can't test this.

        int[] moveStartingPos = piece.getCoordinates();
        int[] moveEndingPos = move.getEndingPosition();

        // find any pieces we've jumped in the process, and remove them as well
        Piece[] jumpedPieces = move.getJumpedPieces(this);
        if (jumpedPieces != null)
        {
            // loop over all jumped pieces and remove them
            for (int i = 0; i < jumpedPieces.length; i++)
            {
                if (jumpedPieces[i] != null) // apparently this can happen... ?????
                {
                    this.setValueAt(jumpedPieces[i].getCoordinates()[0], jumpedPieces[i].getCoordinates()[1], null);
                }
            }
        }

        // and, move this piece (WE PRESUME that it's this piece) from its old spot (both on board and with the piece itself)
        this.setValueAt(moveStartingPos[0], moveStartingPos[1], null);
        piece.moveTo(moveEndingPos[0], moveEndingPos[1]);

        // do a favor to the piece and check if it should now be a king (it'll change itself)
        piece.checkIfShouldBeKing(this);

        // finally, set the move's destination to the piece we're moving
        this.setValueAt(moveEndingPos[0], moveEndingPos[1], piece);
    }

    /**
     * Sets the space at these coordinates to the given Piece object.
     * @param x The x position of the Piece
     * @param y The y position of the Piece
     * @param piece The Piece to put in this space, but can be null to make the space empty
     */
    private void setValueAt(int x, int y, Piece piece)
    {
        this.boardArray[y][x] = piece;
    }

    /**
     * Sets the space at this number position to the given Piece object.
     * @param position The number position, zero indexed at top left.
     * @param piece The Piece to put in this space, but can be null to make the space empty
     */
    private void setValueAt(int position, Piece piece)
    {
        int[] coords = getCoordinatesFromPosition(position); // convert position to coordinates and use that
        this.setValueAt(coords[0], coords[1], piece);
    }

    /**
     * Get's the Piece object at this location. (doesn't error check)
     * @param x The x position of the Piece
     * @param y The y position of the Piece
     * @return The Piece here. (May be null)
     */
    public Piece getValueAt(int x, int y)
    {
        return this.boardArray[y][x];
    }

    /**
     * Get's the Piece object at this location, but using a single number,
     * which progresses from 0 at the top left to the square of the size at the bottom right
     * @param position This number, zero indexed at top left
     * @return The Piece here. (may be null).
     */
    public Piece getValueAt(int position)
    {
        int[] coords = getCoordinatesFromPosition(position); // convert position to coordinates and use that
        return this.getValueAt(coords[0], coords[1]);
    }

    /**
     * Converts a single position value to x and y coordinates.
     * @param position The single position value, zero indexed at top left.
     * @return A two part int array where [0] is the x coordinate and [1] is the y.
     */
    public int[] getCoordinatesFromPosition(int position)
    {
        int[] coords = new int[2];

        // get and use x and y by finding low and high frequency categories
        coords[0] = position % this.size; // x is low frequency
        coords[1] = position / this.size; // y is high frequency
        return coords;
    }

    /**
     * Converts from x and y coordinates to a single position value,
     * which progresses from 0 at the top left to the square of the size minus one at the bottom right
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The single position value.
     */
    public int getPositionFromCoordinates(int x, int y)
    {
        // sum all row for y, and add low frequency x
        return this.size*y + x;
    }

    /**
     * @return Returns true if the given position on the board represents a "BLACK" square on the checkboard.
     * (The checkerboard in this case starts with a "white" space in the upper left hand corner
     * @param x The x location of the space
     * @param y The y location of the space
     */
    public boolean isCheckerboardSpace(int x, int y)
    {
        // this is a checkerboard space if x is even in an even row or x is odd in an odd row
        return x % 2 == y % 2;
    }

    /**
     * @return Returns true if the given coordinates are over the edge the board
     * @param x The x coordinate of the position
     * @param y The y coordinate of the position
     */
    public boolean isOverEdge(int x, int y)
    {
        return (x < 0 || x >= this.size ||
                y < 0 || y >= this.size);
    }

    /**
     * @return Returns true if the given position is over the edge the board
     * @param position The given 0-indexed position value
     */
    public boolean isOverEdge(int position)
    {
        int[] coords = getCoordinatesFromPosition(position); // convert position to coordinates and use that
        return this.isOverEdge(coords[0], coords[1]);
    }

    /**
     * Flips the board coordinates so that the other pieces are on top, etc.
     * @return Returns a new board flipped (doesn't modify this one)
     * @deprecated // this method doesn't seem to work, and there are easier ways to do this
     */
    public Board getFlippedBoard()
    {
        // copy this Board, as the basis for a new, flipped one
        Board newBoard = new Board(this);

        // switch every piece to the one in the opposite corner
        for (int y = 0; y < newBoard.size; y++)
        {
            for (int x = 0; x < newBoard.size; x++)
            {
                // get piece in opposite corner...
                Piece oldPiece = this.getValueAt(this.size - 1 - x, this.size - 1 - y);

                if (oldPiece != null)
                {
                    // ...and transfer color and position to a new generated piece if it exists
                    newBoard.setValueAt(x, y, new Piece(x, y, oldPiece.isWhite));
                }
                else
                {
                    // otherwise just add an empty space
                    newBoard.setValueAt(x, y, null);
                }
            }
        }

        return newBoard;
    }

    @Override
    public String toString() {
        return "Board{" +
                "boardArray=" + Arrays.toString(boardArray) +
                ", size=" + size +
                '}';
    }
}

/**
 * Resposible for communicating with the human player and serving as an interface with the main game engine.
 *
 * @author Mckenna Cisler
 * @version 12.7.2015
 */
class HumanPlayer
{
    // global variables
    Scanner input = new Scanner(System.in);
    boolean isWhite;
    /**
     * Constructor for the HumanPlayer

     */
    public HumanPlayer(boolean isWhite)
    {
        this.isWhite = isWhite;
    }

    /**
     * Gets a move, by asking the human player what move they want to do.
     * @param board The board to apply the move to (assumed to be oriented so that this player is on the top)
     * @return Returns the board, modified according to the player's move
     */
    public Board getMove(Board board, JsonObject jsonObject, String player)
    {
        Gson gson = new Gson();
        System.out.println(gson.toJson(jsonObject));
        // display board to help user (without possible moves)
        displayBoard(board, null, player, jsonObject);

        // keep asking until they select a piece with a valid move
        Move[] possibleMoves;
        while (true)
        {
            // ask user for a piece
            Piece pieceMoving = getPieceFromUser(board);

            // check for quit
            if (pieceMoving == null)
                return board;

            // find all possible moves the player could do
            possibleMoves = pieceMoving.getAllPossibleMoves(board);

            // check that there are some, and if so continue to ask for move
            if (possibleMoves == null)
                System.out.println("That piece has no possible moves! Please choose another:");
            else
            {
                // show the user possible moves and ask for one (user will enter a number)
                displayBoard(board, possibleMoves, player, jsonObject);
                Move move = getMoveFromUser(possibleMoves);

                // apply move to board and return it if the user entered a valid one
                // OTHERWISE, the user requested a retry, so loop again
                if (move != null)
                {
                    board.applyMoveToBoard(move, pieceMoving);
                    return board;
                }
            }
        }
    }

    /**
     * Responsible for displaying the game board to the user (optionally with possible moves)
     * @param board The board to be displayed
     * @param possibleMoves An optional Array of possible moves to display while printing the board.
     * The board will display as normal if this is null.
     */
    private void displayBoard(Board board, Move[] possibleMoves, String player, JsonObject jsonObject)
    {
        // clear the screen for board display
        GameRunner.clearScreen();
        RequestedAction requestedActionForChoosingWhereToGo = new RequestedAction("CLICK", player);
        ArrayList<RequestedAction> listOfRequestedActions = new ArrayList<>(Arrays.asList(requestedActionForChoosingWhereToGo));
        // include a hidden top row for coordinates
        if(possibleMoves != null) {
            for (int y = 0; y < board.size; y++)
            {
                // include a hidden left column for coordinates
                for (int x = 0; x < board.size; x++)
                {
                    // get piece here (possibly null)
                    Piece thisPiece = board.getValueAt(x, y);

                    // if there are any, loop over the possible moves and see if any end at this space
                    if (possibleMoves != null)
                    {
                        // use to determine whether to continue and skip printing other things
                        boolean moveFound = false;

                        for (int i = 0; i < possibleMoves.length; i++)
                        {
                            int[] move = possibleMoves[i].getEndingPosition();
                            if (move[0] == x && move[1] == y)
                            {
                                // here print 2 case green circle;
                                for (Display display: jsonObject.displays) {
                                    display.addContent(new Circle(Integer.toString(move[0] * 50 + 25), Integer.toString(move[1] * 50 + 25), "10", "green"));

                                }
                                // if one here, put the list index (one-indexed) here as a char
                                System.out.print("| " + Integer.toString(i+1) + " ");
                                moveFound = true;
                                Zone tempZone = new Zone(x * 50, y * 50, 50, 50);
                                listOfRequestedActions.get(0).zones.add(tempZone);
                            }
                        }

                        // if a move is found here, skip our other possible printings
                        if (moveFound)
                            continue;
                    }
                }

            }

            jsonObject.requested_actions = listOfRequestedActions;
            Gson gson = new Gson();
            System.out.println(gson.toJson(jsonObject));
            for(Move move : possibleMoves) {
                for (Display display: jsonObject.displays) {
                    display.eliminateGreenCircles(Integer.toString(move.x2), Integer.toString(move.y2));
                }
            }

        }
    }

    /**
     * Asks the user for a piece on the board (for them to move),
     * and ensures it is an actual piece of the correct color
     * @param board The board to check against
     * @return The Piece object to be returned (will be an actual piece)
     */
    private Piece getPieceFromUser(Board board)
    {
        // keep trying again until we get a valid peice chosen
        while (true)
        {
            String raw;
            try
            {
                raw = input.nextLine().toLowerCase();
                Gson gson = new Gson();
                var actions = gson.fromJson(raw, Actions.class);
                System.out.println(actions);
                int x = Integer.parseInt(actions.actions.get(0).x) / 50;
                int y = Integer.parseInt(actions.actions.get(0).y) / 50;
                // ensure there's no out-of-bounds entries
                if (board.isOverEdge(x, y))
                    throw new Exception();

                // now get the actual piece there
                Piece userPiece = board.getValueAt(x, y);

                // and see if it is valid (isn't null and is this player's color)
                if (userPiece == null)
                    System.out.println("There is no piece there!\n");
                else if (userPiece.isWhite != this.isWhite)
                    System.out.println("That's not your piece!\n");
                else
                    return userPiece;
            }
            catch (Exception e) // catch incorrect parse or our throw exception
            {
                System.out.println("Please enter a coordinate on the board in the form '[letter][number]'.");
                continue;
            }
        }
    }

    /**
     * Asks the user for a number representing a move of a particular piece,
     * checking that it is an available move. (The user should be shown all moves beforehand)
     * @param possibleMoves The list of possible moves the user can request
     * @return The Move object representing the chosen move (may be null if the user chooses to get a new piece)
     */
    private Move getMoveFromUser(Move[] possibleMoves)
    {
        int moveNum = 0;

        // keep trying again until we get a valid move chosen
        while (true)
        {
            try
            {
                String raw = input.nextLine().toLowerCase();
                Gson gson = new Gson();
                var actions = gson.fromJson(raw, Actions.class);
                System.out.println(actions);
                int x = Integer.parseInt(actions.actions.get(0).x) / 50;
                int y = Integer.parseInt(actions.actions.get(0).y) / 50;


                for(int i=0 ;i<possibleMoves.length; i++) {
                    if(possibleMoves[i].x2 == x && possibleMoves[i].y2 == y) {
                        moveNum = i+1;
                    }
                }
                // allow user to quit back to another piece by entering 0
                if (moveNum == 0)
                {
                    return null;
                }
                // ensure they enter a move that we printed
                else if (moveNum > possibleMoves.length)
                    throw new Exception();

                // return the move the user entered (switch to 0-indexed), once we get a valid entry
                return possibleMoves[moveNum - 1];
            }
            catch (Exception e) // catch incorrect parse or our throw exception
            {
                input.nextLine(); // compensate for java's annoying issue
            }
        }
    }

    @Override
    public String toString() {
        return "HumanPlayer{" +
                "input=" + input +
                ", isWhite=" + isWhite +
                '}';
    }

    /**
     * @return Returns a titlecase string representing this player's color
     */
    private String getColor()
    {
        return isWhite ? "White" : "Black";
    }
}

/**
 * Represents a single move of a piece.
 *
 * @author Mckenna Cisler
 * @version 12.1.2015
 */
class Move
{
    int x1, y1, x2, y2;
    Move precedingMove;
    boolean isJump;

    /**
     * Constructor for objects of class Move - initializes starting and final position.
     * @param x1 Starting x position.
     * @param y1 Starting y position.
     * @param x2 Ending x position.
     * @param y2 Ending y position.
     * @param precedingMove The move preceding this one (can be null if move is first)
     */
    public Move(int x1, int y1, int x2, int y2, Move precedingMove, boolean isJump)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.precedingMove = precedingMove;
        this.isJump = isJump;
    }

    /**
     * @return Returns a two-part array representing the coordinates of this move's starting position.
     */
    public int[] getStartingPosition()
    {
        int[] position = new int[2];
        position[0] = x1;
        position[1] = y1;
        return position;
    }

    /**
     * @return Returns a two-part array representing the coordinates of this move's ending position.
     */
    public int[] getEndingPosition()
    {
        int[] position = new int[2];
        position[0] = x2;
        position[1] = y2;
        return position;
    }

    /**
     * Finds the pieces jumped in this move.
     * (Get's inbetween jumps using recursion)
     * @return Returns an array of pieces that were jumped.
     * @param board The board to look for the pieces on.
     */
    public Piece[] getJumpedPieces(Board board)
    {
        // if this move wasn't a jump, it didn't jump a piece!
        if (isJump)
        {
            // create expandable list of all pieces
            ArrayList<Piece> pieces = new ArrayList<Piece>();

            // the piece this move is jumping should be between the start and end of this move
            // (the average of those two positions)
            int pieceX = (x1 + x2)/2;
            int pieceY = (y1 + y2)/2;

            // add this most recent jump...
            pieces.add(board.getValueAt(pieceX, pieceY));

            // ...but also go back to get the inbetween ones (if we're not the first move)
            if (precedingMove != null)
            {
                pieces.addAll(Arrays.asList(precedingMove.getJumpedPieces(board)));
                // something is wrong (a preceding move isn't a jump) if this returns null, so let the error be thrown
            }

            // shorten and return
            pieces.trimToSize();
            return pieces.toArray(new Piece[1]); // convert to Piece array
        }
        else
            return null;
    }

    @Override
    public String toString() {
        return "Move{" +
                "x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                ", precedingMove=" + precedingMove +
                ", isJump=" + isJump +
                '}';
    }
}

/**
 * A class representing a game piece, and handling interactions with it.
 *
 * @author Mckenna Cisler
 * @version 11.23.2015
 */
class Piece
{
    public int x;
    public int y;
    public boolean isKing = false;
    public boolean isWhite;

    /**
     * Constructor for objects of class Piece
     * Initializes position and color.
     * @param x The x position of this piece.
     * @param y The y position of this piece.
     * @param isWhite Whether this piece is white or black.
     */
    public Piece(int x, int y, boolean isWhite)
    {
        this.x = x;
        this.y = y;
        this.isWhite = isWhite;
    }

    /**
     * @return Returns a two-part array representing the coordinates of this piece's position.
     */
    public int[] getCoordinates()
    {
        int[] coordinates = new int[2];
        coordinates[0] = this.x;
        coordinates[1] = this.y;
        return coordinates;
    }

    /**
     * @return Returns a string representation of this given piece
     */
    public String getString()
    {
        String baseSymbol;

        if (isWhite)
            baseSymbol = "W";
        else
            baseSymbol = "B";

        if (isKing)
            baseSymbol += "K";
        else
            baseSymbol += " "; // add a space in the non-king state just to keep consistency

        return baseSymbol;
    }

    /**
     * Switches this piece to a king (TODO: MAY BE UNNECCESARY DUE TO BELOW METHOD!!)
     */
    private void setKing()
    {
        isKing = true;
    }

    /**
     * Switches this peice to be a king if it is at the end of the board.
     * Should be called after every move.
     */
    public void checkIfShouldBeKing(Board board)
    {
        // if the piece is white, it's a king if it's at the +y, otherwise if its black this happens at the -y side
        if (isWhite && this.y == board.size - 1 ||
                !isWhite && this.y == 0)
            this.setKing();
    }

    /**
     * Moves this piece's reference of its position (DOES NOT ACTUALLY MOVE ON BOARD)
     * @param x The x coordinate of the move
     * @param y The y coordinate of the move
     */
    public void moveTo(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Generates all physically possible moves of the given piece.
     * (Only actually generates the non-jumping moves - jumps are done recusively in getAllPossibleJumps)
     * @return Returns a list of all the moves (including recusively found jumps), including each individual one involved in every jump.
     * @param board The board to work with - assumed to be flipped to correspond to this piece's color.
     */
    public Move[] getAllPossibleMoves(Board board)
    {
        // create expandable list of all moves
        ArrayList<Move> moves = new ArrayList<Move>();

        // change y endpoints based on kingness and color=direction of movement
        int startingY, yIncrement;
        if (isWhite)
        {
            // if it's white, we move from further down the board backwards to possible king position
            startingY = this.y + 1;
            yIncrement = -2;
        }
        else
        {
            // if it's black, we move from further up the board forward to possible king position
            startingY = this.y - 1;
            yIncrement = 2;
        }

        // use kingess to determine number of rows to check
        int rowsToCheck = 1; // default as non-king
        if (this.isKing)
            rowsToCheck = 2;

        // iterate over the four spaces where normal (non-jumping) moves are possible
        for (int x = this.x - 1; x <= this.x + 1; x += 2)
        {
            // go over the rows (or row) (we iterate the number of times determined by the kingess above)
            int y = startingY - yIncrement; // add this so we can add the normal increment before the boundary checks
            for (int i = 0; i < rowsToCheck; i++)
            {
                // increment y if we need to (this will have no effect if we only run one iteration)
                y += yIncrement;

                // check for going off end of board, in which case just skip this iteration (we may do this twice if at a corner)
                if (board.isOverEdge(x, y))
                    continue;

                // add a move here if there's not a piece
                if (board.getValueAt(x, y) == null)
                {
                    // this is not jump move in any case, and is always the first move
                    moves.add(new Move(this.x, this.y, x, y, null, false));
                }
            }
        }

        // after we've checked all normal moves, look for and add all possible jumps (recusively as well - I mean ALL jumps)
        Move[] possibleJumps = this.getAllPossibleJumps(board, null);
        if (possibleJumps != null)
            moves.addAll(Arrays.asList(possibleJumps));

        // IF there are some moves, shorten and return ArrayList as a normal array
        if (!moves.isEmpty())
        {
            moves.trimToSize();
            return moves.toArray(new Move[1]); // convert to Move objects
        }
        else
            return null; // return null otherwise to symbolize no moves
    }

    /**
     * Finds all jumping moves originating from this piece.
     * Does this recursivly; for each move a new imaginary piece will be generated,
     * and this function will then be called on that piece to find all possible subsequent moves.
     * @param board The board to work with - assumed to be flipped to correspond to this piece's color.
     * @param precedingMove The moves preceding the call to search for moves off this piece - only used
     * in recursion, should be set to null at first call. (if it's not, it means this piece is imaginary).
     */
    private Move[] getAllPossibleJumps(Board board, Move precedingMove)
    {
        // create expandable list of all moves
        ArrayList<Move> moves = new ArrayList<Move>();

        // this is the same as above except we're doing a large cube (4x4)
        // change y endpoints based on kingness and color=direction of movement
        int startingY, yIncrement;
        if (isWhite)
        {
            // if it's white, we move from further down the board backwards to possible king position
            startingY = this.y + 2;
            yIncrement = -4;
        }
        else
        {
            // if it's black, we move from further up the board forward to possible king position
            startingY = this.y - 2;
            yIncrement = 4;
        }

        // use kingess to determine number of rows to check
        int rowsToCheck = 1; // default as non-king
        if (this.isKing)
            rowsToCheck = 2;

        // iterate over the four spaces where normal (non-jumping) moves are possible
        for (int x = this.x - 2; x <= this.x + 2; x += 4)
        {
            // go over the rows (or row) (we iterate the number of times determined by the kingess above)
            int y = startingY - yIncrement; // add this so we can add the normal increment before the boundary checks in the loop
            for (int i = 0; i < rowsToCheck; i++)
            {
                // increment y if we need to (this will have no effect if we only run one iteration)
                y += yIncrement;

                // check for going off end of board, in which case just skip this iteration (we may do this twice if at a corner)
                if (board.isOverEdge(x, y))
                    continue;

                // don't try to go backward to our old move start so we don't get in infinite recursion loops
                if (precedingMove != null &&
                        x == precedingMove.getStartingPosition()[0] &&
                        y == precedingMove.getStartingPosition()[1])
                    continue;

                // test if there is a different-colored piece between us (at the average of our position) and the starting point
                // AND that there's no piece in the planned landing space (meaning we can possible jump there)
                Piece betweenPiece = board.getValueAt( (this.x + x)/2 , (this.y + y)/2 );
                if (betweenPiece != null &&
                        betweenPiece.isWhite != this.isWhite &&
                        board.getValueAt(x, y) == null)
                {
                    // in which case, add a move here, and note that it is a jump (we may be following some other jumps)
                    Move jumpingMove = new Move(this.x, this.y, x, y, precedingMove, true); // origin points are absolute origin (ORIGINAL piece)

                    // then add it to our list
                    moves.add(jumpingMove);

                    // after jumping, create an imaginary piece as if it was there to look for more jumps
                    Piece imaginaryPiece = new Piece(x, y, this.isWhite);

                    // correspond possible jumps to this piece's kingness
                    if (this.isKing) imaginaryPiece.setKing();

                    // find possible subsequent moves recusivly
                    Move[] subsequentMoves = imaginaryPiece.getAllPossibleJumps(board, jumpingMove);

                    // add these moves to our list if they exist, otherwise just move on to other possibilities
                    if (subsequentMoves != null)
                        moves.addAll(Arrays.asList(subsequentMoves));
                }
            }
        }

        // IF there are some moves, shorten and return ArrayList as a normal array
        if (!moves.isEmpty())
        {
            moves.trimToSize();
            return moves.toArray(new Move[1]); // convert to Move arrays
        }
        else
            return null; // return null otherwise to symbolize no moves
    }

    @Override
    public String toString() {
        return "Piece{" +
                "x=" + x +
                ", y=" + y +
                ", isKing=" + isKing +
                ", isWhite=" + isWhite +
                '}';
    }
}


/**
 * An abstract version of a player, from which Human and AI Players will be extended.
 * Used so that both player types can be used interchangably.
 *
 * @author Mckenna Cisler
 * @version 11.23.2015
 */


class JsonObject {
    public List<Display> displays = Arrays.asList(new Display(1), new Display(2));
    public ArrayList<RequestedAction> requested_actions = new ArrayList<>(List.of(new RequestedAction("", "")));
    public GameState game_state = new GameState();

    public void addAction(RequestedAction requestedAction) {
        this.requested_actions.add(requestedAction);
    }

    @Override
    public String toString() {
        return "JsonObject{" +
                "displays=" + displays +
                ", requested_actions=" + requested_actions +
                ", game_state=" + game_state +
                '}';
    }
}

class Display {
    public String width = "450";
    public String height = "450";
    public List<DisplayContent> content = new ArrayList<>(Arrays.asList(
            new DisplayContent("style", "line{stroke:black;stroke-width:4;}"),
            new Line("0", "50", "450", "50"),
            new Line("0", "100", "450", "100"),
            new Line("0", "150", "450", "150"),
            new Line("0", "200", "450", "200"),
            new Line("0", "250", "450", "250"),
            new Line("0", "300", "450", "300"),
            new Line("0", "350", "450", "350")
    ));
    public int player;

    public Display(int player) {
        this.player = player;
    }

    public void addContent(DisplayContent displayContent) {
        content.add(displayContent);
    }

    public void eliminateGreenCircles(String cx, String cy) {
        for (int i=0; i< this.content.size(); i++) {
            if(this.content.get(i).equals(new Circle(cx, cy, "10", "green"))) {
                this.content.remove(this.content.get(i));
            }
        }
    }
}

class DisplayContent {
    public String tag;
    public String content;

    public DisplayContent(String tag, String content) {
        this.tag = tag;
        this.content = content;
    }
}

class Line extends DisplayContent {
    public String x1;
    public String y1;
    public String x2;
    public String y2;

    public Line(String x1, String y1, String x2, String y2) {
        super("line", null);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
}

class Circle extends DisplayContent {
    public String cx;
    public String cy;
    public String r;
    public String fill;

    public Circle(String cx, String cy, String r, String fill) {
        super("circle", null);
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.fill = fill;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Circle circle)) return false;
        return Objects.equals(cx, circle.cx) && Objects.equals(cy, circle.cy) && Objects.equals(r, circle.r) && Objects.equals(fill, circle.fill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cx, cy, r, fill);
    }
}

class RequestedAction {
    public String type;
    public String player;
    public ArrayList<Zone> zones = new ArrayList<>();

    public RequestedAction(String type, String player) {
        this.type = type;
        this.player = player;
    }

    public List<Zone> getZones() {
        return zones;
    }

    public void setZones(ArrayList<Zone> zones) {
        this.zones = zones;
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }
}

class Zone {
    public int x;
    public int y;
    public int width;
    public int height;

    public Zone(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}

class GameState {
    public List<Integer> scores = Arrays.asList(0,0);
    public boolean game_over = false;

}

class Actions {
    public ArrayList<Action> actions = new ArrayList<>();

    @Override
    public String toString() {
        return "Actions{" +
                "actions=" + actions +
                '}';
    }
}

class Action {
    public String x;
    public String y;
    public String player;

    public Action(String x, String y, String player) {
        this.x = x;
        this.y = y;
        this.player = player;
    }

    @Override
    public String toString() {
        return "Action{" +
                "x='" + x + '\'' +
                ", y='" + y + '\'' +
                ", player='" + player + '\'' +
                '}';
    }
}

