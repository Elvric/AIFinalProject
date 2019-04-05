package student_player;

import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoCoord;
import pentago_swap.PentagoMove;
import sun.nio.fs.MacOSXFileSystemProvider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MyTools {

  private static MyTools instance = new MyTools();
  private int color;
  private Node rootNode;
  private int limit = 3;
  private double RATIO = 0.8;
  private final double ALPHA = 0.8;
  private long timeToThink = 1080;
  private int hestimate = 1800;

  private MyTools() {}

  public static MyTools getInstance(PentagoBoardState state) {
    instance.color = state.getTurnPlayer();
    return instance;
  }

  public PentagoMove monteCarlo(PentagoBoardState state) {
    long startTime = System.currentTimeMillis();
    long timeEnd = System.currentTimeMillis() + timeToThink;
    if (state.getTurnNumber() == 0) {
      this.limit = 3;
      this.timeToThink = 1080;
      this.hestimate = 1800;
      return firstMove(state);
    }
    if (state.getTurnNumber() == 1) {
      timeToThink -= 50;
    } else if (state.getTurnNumber() == 7) {
      limit++;
    } else if (state.getTurnPlayer() == 14) {
      limit = Integer.MAX_VALUE;
    }
    rootNode = new Node(state, null, 0, 0);
    while (System.currentTimeMillis() < timeEnd) {
      monteCarloStimulation(descentPhase());
    }
    System.out.println(rootNode.visitCount);
    PentagoMove move = getNextBestMove();
    System.out.println(rootNode.visitCount);
    hestimate = (int) ((System.currentTimeMillis() - startTime) * ALPHA + hestimate * (1 - ALPHA));
    if (hestimate > 1900) {
      timeToThink -= Integer.max(3, (int) ((hestimate - 1900) * 0.3));
    } else if (hestimate < 1600) {
      timeToThink += Integer.max(4, (int) ((1600 - hestimate) * 0.1));
    }
    System.out.println(timeToThink);
    return move;
  }

  public Node descentPhase() {
    Node candidate = rootNode;
    while (!candidate.children.isEmpty()) {
      double bestScore = Integer.MIN_VALUE;
      for (Node child : candidate.children.keySet()) {
        double childScore = getSearchScore(child);
        if (childScore > bestScore) {
          bestScore = childScore;
          candidate = child;
        }
      }
    }
    return candidate;
  }

  public void monteCarloStimulation(Node leaf) {
    if (leaf.state.gameOver()) {
      leaf.incrementUp(
          leaf.state.getWinner() == this.color, leaf.state.getWinner() != 1 - this.color);
      return;
    } else if (leaf.state.getTurnPlayer() != color && leaf.heristic > 1) {
      leaf.incrementUp(false, false);
      return;
    } else if (leaf.state.getTurnPlayer() == color && leaf.heristic > 1) {
      leaf.incrementUp(true, false);
      return;
    }
    leaf.generateChildren();
    if (!leaf.children.isEmpty()) {
      int childNum = new Random().nextInt(leaf.children.size());
      Node stimulation = (Node) leaf.children.keySet().toArray()[childNum];
      PentagoBoardState stimulationState = (PentagoBoardState) stimulation.state.clone();
      while (!stimulationState.gameOver()) {
        stimulationState.processMove(((PentagoMove) stimulationState.getRandomMove()));
      }
      stimulation.incrementUp(
          stimulationState.getWinner() == this.color,
          stimulationState.getWinner() != 1 - this.color);
    }
  }

  public PentagoMove getNextBestMove() {
    Node bestNode = null;
    double bestAverage = Integer.MIN_VALUE;
    for (Node child : rootNode.children.keySet()) {
      if (child.heristic > 0) {
        bestNode = child;
        break;
      } else if (child.heristic < 0 || child.visitCount < 1) {
        continue;
      }
      double average = child.winCount / child.visitCount;
      if (average > bestAverage) {
        bestNode = child;
        bestAverage = average;
      }
    }
    if (bestNode == null) {
      return (PentagoMove) rootNode.state.getRandomMove();
    }
    PentagoMove move = rootNode.children.get(bestNode);
    rootNode = bestNode;
    rootNode.printChildren();
    return move;
  }

  public double getSearchScore(Node n) {
    int nodeNumVisits = n.visitCount;
    if (nodeNumVisits < 1) {
      return 2000;
    }
    int parentVisitCount = n.parent.visitCount;
    double nodeNumWins = n.winCount;
    return nodeNumWins / (double) nodeNumVisits
        + RATIO * Math.sqrt(Math.log(parentVisitCount) / (double) nodeNumVisits);
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

  public boolean fourInARowForMySelf(PentagoBoardState state) {
    int oponent = state.getTurnPlayer();
    // Row verification for 4 in row of opponent
    for (int i = 0; i < 6; i++) {
      int numInRow = 0;
      for (int j = 1; j < 5; j++) {
        PentagoBoardState.Piece piece = state.getPieceAt(i, j);
        if (j == 1 && isOponentPiece(state.getPieceAt(i, 0), oponent)) {
          break;
        } else if (j == 4 && isOponentPiece(state.getPieceAt(i, 5), oponent)) {
          break;
        } else if (isMyPiece(piece, oponent)) {
          numInRow++;
        }
        if (numInRow > 3) {
          return true;
        }
      }
    }
    // Column verification for 4 in row of opponent
    for (int i = 0; i < 6; i++) {
      int numInRow = 0;
      for (int j = 1; j < 5; j++) {
        PentagoBoardState.Piece piece = state.getPieceAt(j, i);
        if (j == 1 && isOponentPiece(state.getPieceAt(0, i), oponent)) {
          break;
        } else if (j == 4 && isOponentPiece(state.getPieceAt(5, i), oponent)) {
          break;
        } else if (isMyPiece(piece, oponent)) {
          numInRow++;
        }
        if (numInRow > 3) {
          return true;
        }
      }
    }
    // Top diag
    int numInRow = 0;
    for (int i = 1; i < 4; i++) {
      PentagoBoardState.Piece piece = state.getPieceAt(i, i);
      if (i == 1 && isOponentPiece(state.getPieceAt(0, 0), oponent)) {
        break;
      } else if (i == 4 && isOponentPiece(state.getPieceAt(5, 5), oponent)) {
        break;
      } else if (isMyPiece(piece, oponent)) {
        numInRow++;
      }
      if (numInRow > 3) {
        return true;
      }
    }

    // Bottom Diag
    numInRow = 0;
    for (int i = 1; i < 4; i++) {
      PentagoBoardState.Piece piece = state.getPieceAt(5 - i, i);
      if (i == 1 && isOponentPiece(state.getPieceAt(5, 0), oponent)) {
        break;
      } else if (i == 4 && isOponentPiece(state.getPieceAt(0, 5), oponent)) {
        break;
      } else if (isMyPiece(piece, oponent)) {
        numInRow++;
      }
      if (numInRow > 3) {
        return true;
      }
    }
    return false;
  }

  public boolean fourInARowForOponent(PentagoBoardState state) {
    int oponent = state.getTurnPlayer();
    // Row verification for 4 in row of opponent
    for (int i = 0; i < 6; i++) {
      int numOponentPiece = 0;
      for (int j = 0; j < 6; j++) {
        PentagoBoardState.Piece piece = state.getPieceAt(i, j);
        if (isOponentPiece(piece, oponent)) {
          numOponentPiece++;
        } else if (isMyPiece(piece, oponent)) {
          if (j != 0 || j != 5) {
            break;
          } else if (isMyPiece(state.getPieceAt(i, 5 - j), oponent)) {
            break;
          }
        }
        if (numOponentPiece > 3) {
          return true;
        }
      }
    }
    // column verification for opponent
    for (int i = 0; i < 6; i++) {
      int numOponentPiece = 0;
      for (int j = 0; j < 6; j++) {
        PentagoBoardState.Piece piece = state.getPieceAt(j, i);
        if (isOponentPiece(piece, oponent)) {
          numOponentPiece++;
        } else if (isMyPiece(piece, oponent)) {
          if (j != 0 || j != 5) {
            break;
          } else if (isMyPiece(state.getPieceAt(5 - j, i), oponent)) {
            break;
          }
        }
        if (numOponentPiece > 3) {
          return true;
        }
      }
    }
    int numOponentPiece = 0;
    for (int i = 0; i < 6; i++) {
      PentagoBoardState.Piece piece = state.getPieceAt(i, i);
      if (isOponentPiece(piece, oponent)) {
        numOponentPiece++;
      } else if (isMyPiece(piece, oponent)) {
        if (i != 0 || i != 5) {
          break;
        } else if (isMyPiece(state.getPieceAt(5 - i, 5 - i), oponent)) {
          break;
        }
      }
      if (numOponentPiece > 3) {
        return true;
      }
    }
    numOponentPiece = 0;
    for (int i = 0; i < 6; i++) {
      PentagoBoardState.Piece piece = state.getPieceAt(5 - i, i);
      if (isOponentPiece(piece, oponent)) {
        numOponentPiece++;
      } else if (isMyPiece(piece, oponent)) {
        if (i != 0 || i != 5) {
          break;
        } else if (isMyPiece(state.getPieceAt(i, 5 - i), oponent)) {
          break;
        }
      }
      if (numOponentPiece > 3) {
        return true;
      }
    }
    return false;
  }

  public int[] oponentHasDangerousThreeInRow(PentagoBoardState state) {
    int oponent = 1-state.getTurnPlayer();
    int[] array = {-1,-1,-1,-1};
    for (int i = 0; i < 6; i++) {
      int numInRow = 0;
      int importantLoctation = -1;
      for (int j = 0; j < 6; j++) {
        PentagoBoardState.Piece piece = state.getPieceAt(i, j);
        if (isMyPiece(piece, oponent)) {
          numInRow = 0;
          break;
        } else if (isOponentPiece(piece, oponent)
            && (importantLoctation == -1 || j == importantLoctation || j == 1 || j == 4)) {
          numInRow++;
        }
        if (numInRow == 2 && importantLoctation == -1) {
          switch (j) {
            case 2:
              importantLoctation = 3;
              break;
            case 1:
              importantLoctation = 5;
              break;
            case 3:
              importantLoctation = 4;
          }
        }
      }
      if (numInRow > 2) {
        array[0] = i;
        return array;
      }
    }
    for (int i = 0; i < 6; i++) {
      int numInRow = 0;
      int importantLoctation = -1;
      for (int j = 0; j < 6; j++) {
        PentagoBoardState.Piece piece = state.getPieceAt(j, i);
        if (isMyPiece(piece, oponent)) {
          numInRow = 0;
          break;
        } else if (isOponentPiece(piece, oponent)
                && (importantLoctation == -1 || j == importantLoctation || j == 1 || j == 4)) {
          numInRow++;
        }
        if (numInRow == 2 && importantLoctation == -1) {
          switch (j) {
            case 2:
              importantLoctation = 3;
              break;
            case 1:
              importantLoctation = 5;
              break;
            case 3:
              importantLoctation = 4;
          }
        }
      }
      if (numInRow > 2) {
        array[1] = i;
        return array;
      }
    }
    return array;
  }

  public boolean isOponentPiece(PentagoBoardState.Piece piece, int oponent) {
    if (piece == PentagoBoardState.Piece.EMPTY) {
      return false;
    } else if (piece == PentagoBoardState.Piece.WHITE && oponent == 0) {
      return true;
    } else if (piece == PentagoBoardState.Piece.BLACK && oponent == 1) {
      return true;
    }
    return false;
  }

  public boolean isMyPiece(PentagoBoardState.Piece piece, int oponent) {
    return isOponentPiece(piece, 1 - oponent);
  }

  public double getHeuristic(PentagoBoardState state) {
    double answer = 0;
    boolean gameOver = state.gameOver();
    if (gameOver) {
      if (state.getWinner() == color) {
        answer = Integer.MAX_VALUE;
      } else if (state.getWinner() == 1 - color) {
        answer = Integer.MIN_VALUE;
      }
    }
    if (fourInARowForOponent(state) && !gameOver) {
      return Integer.MIN_VALUE;
    } else if (fourInARowForMySelf(state) && !gameOver) {
      return 2;
    }
    if (state.getTurnPlayer() == color) {
      answer = -answer;
    }
    return answer;
  }

  class Node {
    Node parent;
    PentagoBoardState state;
    Map<Node, PentagoMove> children = new HashMap<>();
    double winCount = 0;
    int visitCount = 0;
    int depth;
    double heristic;
    boolean childrenGenerate = false;

    public Node(PentagoBoardState state, Node parent, int depth, double heristic) {
      this.state = state;
      this.parent = parent;
      this.depth = depth;
      this.heristic = heristic;
    }

    public void generateChildren() {
      if (childrenGenerate) {
        return;
      }
      childrenGenerate = true;
      Node bestNode = null;
      PentagoMove bestMove = null;
      int depth = this.depth + 1;
      List<PentagoMove> moves;
      if (state.getTurnNumber() < 2) {
        moves = firstthreeMoves(state);
      } else {
        moves = depth > limit ? allValidMoveIgnoreFlips(state) : state.getAllLegalMoves();
      }
      for (PentagoMove move : moves) {
        PentagoBoardState childState = (PentagoBoardState) this.state.clone();
        childState.processMove(move);
        double heuristicChild = getHeuristic(childState);
        if (heuristicChild > 0 && (bestNode == null || bestNode.heristic < heuristicChild)) {
          bestNode = new Node(childState, this, depth, heuristicChild);
          bestMove = move;
          if (heuristicChild > 500) {
            break;
          }
        } else if (heuristicChild < 0) {
          continue;
        } else {
          Node child = new Node(childState, this, depth, heuristicChild);
          children.put(child, move);
        }
      }
      if (bestNode != null) {
                if (bestNode.heristic > 500) {
                  children.clear();
                  children.put(bestNode, bestMove);
                }
                else {
                  bestNode.generateChildren();
                  if (!(bestNode.children.size() > 1)) {
                    children.clear();
                    children.put(bestNode, bestMove);
                  }
                }
//        children.clear();
//        children.put(bestNode, bestMove);
      }
      if (this == rootNode && children.size() > 1 && state.getTurnNumber() > 1) {
        verifyThreeInRow();
      }
    }

    public void verifyThreeInRow() {
      int[] array = oponentHasDangerousThreeInRow(this.state);
      List<PentagoMove> moves = new ArrayList<>();
      for (int i = 0; i < array.length; i++) {
        if (array[i] > -1) {
          if (i == 0) {
            for (int j = 0; j < 6; j++) {
              PentagoCoord cord = new PentagoCoord(array[i],j);
              if (state.isPlaceLegal(cord)) {
                moves.add(new PentagoMove(cord, PentagoBoardState.Quadrant.TL, PentagoBoardState.Quadrant.TR, state.getTurnPlayer()));
              }
            }
          }
          else if (i == 1) {
            for (int j = 0; j < 6; j++) {
              PentagoCoord cord = new PentagoCoord(j,array[i]);
              if (state.isPlaceLegal(cord)) {
                moves.add(new PentagoMove(cord, PentagoBoardState.Quadrant.TL, PentagoBoardState.Quadrant.TR, state.getTurnPlayer()));
              }
            }
          }
          break;
        }
      }
      if (moves.isEmpty()) {
        return;
      }
      children.clear();
      for (PentagoMove move : moves) {
        PentagoBoardState childState = (PentagoBoardState) this.state.clone();
        childState.processMove(move);
        Node child = new Node(childState, this, depth, 0);
        children.put(child,move);
        }
    }
    public void incrementUp(boolean won, boolean draw) {
      if (won) {
        winCount++;
      } else if (draw) {
        winCount += 0.5;
      }
      visitCount++;
      if (parent != null) {
        parent.incrementUp(won, draw);
      }
    }

    public void printChildren() {
      try {
        PentagoBoardState.Piece piece =
            color == 1 ? PentagoBoardState.Piece.BLACK : PentagoBoardState.Piece.WHITE;
        BufferedWriter writer = new BufferedWriter(new FileWriter("player" + piece + ".txt"));
        writer.write(" ");
        writer.close();
        writer = new BufferedWriter(new FileWriter("player" + piece + ".txt", true));
        writer.write(this.winCount + " " + this.visitCount + "\n");
        writer.write(state.toString());
        for (Node child : children.keySet()) {
          writer.write(child.state.toString());
        }
        writer.close();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
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

  private List<PentagoMove> firstthreeMoves(PentagoBoardState state) {
    List<PentagoMove> moves = new ArrayList<>(18);
    for (int i = 0; i < 6; i++) {
      for (int j = i % 3 == 1 ? 0 : 1; j < 6; j += i % 3 == 1 ? 1 : 3) {
        PentagoCoord cord = new PentagoCoord(i, j);
        if (state.isPlaceLegal(cord)) {
          List<PentagoBoardState.Quadrant> list = new ArrayList<>(4);
          list.add(PentagoBoardState.Quadrant.TR);
          list.add(PentagoBoardState.Quadrant.TL);
          list.add(PentagoBoardState.Quadrant.BL);
          list.add(PentagoBoardState.Quadrant.BR);
          while (list.size() > 1) {
            PentagoBoardState.Quadrant currentQuad = list.remove(0);
            for (PentagoBoardState.Quadrant nextQuad : list) {
              moves.add(new PentagoMove(cord, currentQuad, nextQuad, state.getTurnPlayer()));
            }
          }
        }
      }
    }
    return moves;
  }
}
