package student_player;

import org.omg.CORBA.INTERNAL;
import org.omg.PortableInterceptor.INACTIVE;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoCoord;
import pentago_swap.PentagoMove;

import java.util.*;

public class MyTools {

  private static MyTools instance = new MyTools();
  private int color;
  private Node rootNode;
  private int limit = 2;
  private double RATIO = 0.8;
  private final double ALPHA = 0.7;
  private long timeToThink = 1080;
  private int hestimate = 1800;

  private MyTools() {}

  public static MyTools getInstance(PentagoBoardState state) {
    instance.color = state.getTurnPlayer();
    return instance;
  }

  public PentagoMove monteCarlo(PentagoBoardState state) {
    long t = System.currentTimeMillis();
    long end = t + timeToThink;
    if (state.getTurnNumber() == 0) {
      return firstMove(state);
    }
    rootNode = new Node(state, null, 0, 0);
    if (state.getTurnNumber() > 7) {
      limit = 3;
    }
    while (System.currentTimeMillis() < end) {
      Node leafNode = descent(rootNode);
      rolloutAndPropagationPhase(leafNode);
    }
    System.out.println(rootNode.visitCount);
    PentagoMove nextMove = getNextBestMove();
    System.out.println(rootNode.visitCount);
    long timeTaken = System.currentTimeMillis() - t;
    hestimate = (int) (timeTaken * ALPHA + hestimate * (1 - ALPHA));
    long diff = 2000 - hestimate;
    if (diff < 75) {
      timeToThink -= Math.max(2, diff * 0.2);
    } else if (diff > 500) {
      diff = diff - 500;
      timeToThink += Math.max(3, diff * 0.2);
    }
    System.out.println(hestimate);
    return nextMove;
  }

  private int heuristic(PentagoBoardState state) {
    if (state.gameOver()) {
      if (state.getWinner() == color) {
        return Integer.MAX_VALUE;
      } else if (state.getWinner() > 2) {
        return 0;
      } else {
        return Integer.MIN_VALUE;
      }
    }
    if (enemyHasFourInRow(state) && state.getTurnPlayer() != color) {
      return Integer.MIN_VALUE;
    }
    return 0;
  }

  private boolean enemyHasFourInRow(PentagoBoardState state) {
    int diagMiddleLeftVal = 0;
    int diagMiddleRightVal = 0;
    for (int i = 0; i < 6; i++) {
      int rowValEnemy = 0;
      int colValEnemy = 0;
      for (int j = 0; j < 6; j++) {
        PentagoBoardState.Piece rowPiece = state.getPieceAt(i, j);
        PentagoBoardState.Piece colPiece = state.getPieceAt(j, i);
        if (isMyPiece(rowPiece)) {
          if (j != 0 || j != 5) {
            rowValEnemy = -10;
          } else if (isMyPiece(state.getPieceAt(i, 5 - j))) {
            rowValEnemy = -10;
          }
        } else {
          rowValEnemy += pieceValue(rowPiece);
        }
        if (isMyPiece(colPiece)) {
          if (j != 0 || j != 5) {
            colValEnemy = -10;
          } else if (isMyPiece(state.getPieceAt(5 - j, i))) {
            colValEnemy = -10;
          }
        } else {
          colValEnemy += pieceValue(colPiece);
        }
      }
      if (rowValEnemy > 2 || colValEnemy > 2) {
        return true;
      }
      PentagoBoardState.Piece diagMiddleLeft = state.getPieceAt(i, i);
      PentagoBoardState.Piece digMiddleRight = state.getPieceAt(5-i, i);
      if (isMyPiece(diagMiddleLeft)) {
        if (i != 0 || i != 5) {
          diagMiddleLeftVal = -10;
        }
        else if (isMyPiece(state.getPieceAt(5-i,5-i))) {
          diagMiddleLeftVal = -10;
        }
        else {
          diagMiddleLeftVal += pieceValue(diagMiddleLeft);
        }
      }
      if (isMyPiece(digMiddleRight)) {
        if (i != 0 || i != 5) {
          diagMiddleRightVal = -10;
        }
        else if (isMyPiece(state.getPieceAt(i,5-i))) {
          diagMiddleRightVal = -10;
        }
        else {
          diagMiddleRightVal += pieceValue(diagMiddleLeft);
        }
      }
//      PentagoBoardState.Piece diagUplLeft = state.getPieceAt(i+1, i);
//      PentagoBoardState.Piece diagDownLeft = state.getPieceAt(i, i+1);
//      PentagoBoardState.Piece diagUpRight = state.getPieceAt(4 - i, i);
//      PentagoBoardState.Piece diagDownRight = state.getPieceAt(5 - i, i+1);
    }
    if (diagMiddleLeftVal > 2 || diagMiddleRightVal > 2) {
      return true;
    }
    return false;
  }

  private boolean isMyPiece(PentagoBoardState.Piece piece) {
    if (color == 1 && piece == PentagoBoardState.Piece.BLACK) {
      return true;
    } else if (color == 0 && piece == PentagoBoardState.Piece.WHITE) {
      return true;
    }
    return false;
  }

  private boolean isEnemyPiece(PentagoBoardState.Piece piece) {
    if (piece == PentagoBoardState.Piece.EMPTY) {
      return false;
    }
    if (color == 1 && piece == PentagoBoardState.Piece.BLACK) {
      return false;
    } else if (color == 0 && piece == PentagoBoardState.Piece.WHITE) {
      return false;
    }
    return true;
  }

  private int pieceValue(PentagoBoardState.Piece piece) {
    if (piece == PentagoBoardState.Piece.EMPTY) {
      return 0;
    }
    if (color == 1 && piece == PentagoBoardState.Piece.BLACK) {
      return -1;
    } else if (color == 0 && piece == PentagoBoardState.Piece.WHITE) {
      return -1;
    }
    return 1;
  }

  public double getSearchScore(Node n) {
    int nodeNumVisits = n.visitCount;
    if (nodeNumVisits < 1) {
      return Integer.MAX_VALUE;
    }
    int parentVisitCount = n.parent.visitCount;
    double nodeNumWins = n.winCount;
    return nodeNumWins / (double) nodeNumVisits
        + RATIO * Math.sqrt(Math.log(parentVisitCount) / (double) nodeNumVisits);
  }

  public PentagoMove getNextBestMove() {
    Node currentBest = null;
    double currentBestScore = Integer.MIN_VALUE;
    for (Node child : rootNode.children.keySet()) {
        if (child.heuristic > 0) {
          return rootNode.children.get(child);
        }
        else if (child.heuristic < 0) {
          continue;
        }
        if (child.visitCount < 1) {
          continue;
        }
        double averageWin = child.winCount / child.visitCount;
        if (averageWin > currentBestScore) {
          currentBest = child;
          currentBestScore = averageWin;
        }
    }
    if (currentBest == null) {
      return (PentagoMove) rootNode.state.getRandomMove();
    }
    PentagoMove move = rootNode.children.get(currentBest);
    rootNode = currentBest;
    return move;
  }

  public Node descent(Node startNode) {
    Node leafNode = rootNode;
    // TODO deal with issue where we will always loose on all child
    while (!leafNode.children.isEmpty()) {
      double bestScore = Integer.MIN_VALUE;
      for (Node child : leafNode.children.keySet()) {
        double score = getSearchScore(child);
        if (score > bestScore) {
          leafNode = child;
          bestScore = score;
        }
      }

    }
    return leafNode;
  }

  public void rolloutAndPropagationPhase(Node n) {
    n.generateChildren();
    if (n.children.isEmpty()) {
      return;
    }
    int location = new Random().nextInt(n.children.size());
    Node childExpended = (Node) n.children.keySet().toArray()[location];
    PentagoBoardState playState = (PentagoBoardState) childExpended.state.clone();
    while(!playState.gameOver()) {
      playState.processMove(((PentagoMove) playState.getRandomMove()));
    }
    childExpended.incrementVisits(color == playState.getWinner(), playState.getWinner() > 2);
  }

  class Node {
    private PentagoBoardState state;
    int visitCount = 0;
    double winCount = 0;
    int depth;
    int heuristic;
    Node parent;
    Map<Node, PentagoMove> children = new HashMap<>();

    public Node(PentagoBoardState state, Node parent, int depth, int heuristic) {
      this.state = state;
      this.parent = parent;
      this.depth = depth;
      this.heuristic = heuristic;
    }

    public void generateChildren() {
      int depth = this.depth + 1;
      List<PentagoMove> moves =
          depth > limit ? allValidMoveIgnoreFlips(state) : state.getAllLegalMoves();
      for (PentagoMove move : moves) {
        PentagoBoardState childState = (PentagoBoardState) this.state.clone();
        childState.processMove(move);
        int heuristic = heuristic(childState);
        if (heuristic < 0) {
          continue;
        }
        Node childNode = new Node(childState, this, depth, heuristic);
        if (heuristic > 0) {
          children.clear();
          children.put(childNode, move);
          break;
        }
        children.put(childNode, move);
      }
    }

    public void incrementVisits(boolean hasWon, boolean isDraw) {
      if (hasWon) {
        winCount++;
      } else if (isDraw) {
        winCount += 0.5;
      }
      visitCount++;
      if (this.parent != null) {
        parent.incrementVisits(hasWon, isDraw);
      }
    }
  }

  private PentagoMove firstMove(PentagoBoardState state) {
    int[] cord = {1, 4};
    for (int i : cord) {
      for (int j : cord) {
        PentagoMove move =
            new PentagoMove(
                i, j, PentagoBoardState.Quadrant.TL, PentagoBoardState.Quadrant.TR, color);
        if (state.isLegal(move)) {
          return move;
        }
      }
    }
    return (PentagoMove) state.getRandomMove();
  }

  private List<PentagoMove> allValidMoveIgnoreFlips(PentagoBoardState state) {
    List<PentagoMove> moves = new ArrayList<>(18);
    for (int i = 0; i < 6; i++) {
      for (int j = 0; j < 6; j++) {
        PentagoCoord cord = new PentagoCoord(i, j);
        if (state.isPlaceLegal(cord)) {
          PentagoBoardState.Quadrant[] array = {
            PentagoBoardState.Quadrant.TR,
            PentagoBoardState.Quadrant.TL,
            PentagoBoardState.Quadrant.BL,
            PentagoBoardState.Quadrant.BR
          };
          List<PentagoBoardState.Quadrant> list = Arrays.asList(array);
          Collections.shuffle(list);
          moves.add(new PentagoMove(cord, list.get(0), list.get(1), state.getTurnPlayer()));
        }
      }
    }
    return moves;
  }
}
