package student_player;

import boardgame.BoardState;
import boardgame.Move;
import pentago_swap.PentagoBoard;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;

import java.util.*;

public class MyTools {

  private static MyTools instance;
  private int color;

  private MyTools(int color) {
    this.color = color;
  }

  public static MyTools getInstance(int color) {
    if (instance == null) {
      instance = new MyTools(color);
    }
    return instance;
  }

  public int heuristics(PentagoBoardState pentagoBoardState) {
    if (pentagoBoardState.gameOver()) {
      if (pentagoBoardState.getWinner() == color) {
        return 100;
      } else {
        return -100;
      }
    }
    int rowWite = 0;
    int rowBlack = 0;
    int countWhite;
    int countBlack;
    for (int x = 0; x < 6; x++) {
      countWhite = 0;
      countBlack = 0;
      for (int y = 0; y < 6; y++) {
        PentagoBoardState.Piece atLocation = pentagoBoardState.getPieceAt(x, y);
        if (atLocation == PentagoBoardState.Piece.BLACK) {
          countBlack++;
          if (countWhite > 2) rowWite++;
          countWhite = 0;
        } else if (atLocation == PentagoBoardState.Piece.WHITE) {
          countWhite++;
          if (countBlack > 2) rowBlack++;
          countBlack = 0;
        }
      }
      if (countBlack > 2) {
        rowBlack++;
      }
      if (countWhite > 2) {
        rowWite++;
      }
    }
    int answer = rowWite - rowBlack;

    if (color == 1) answer = -answer;
    return answer;
  }

  public PentagoMove minMaxSearch(PentagoBoardState pentagoBoardState) {
    long t = System.currentTimeMillis();
    long end = t + 1000;
    LinkedList<Node> list = new LinkedList<>();
    Node startNode = new Node(pentagoBoardState);
    list.addLast(startNode);
    while (!list.isEmpty() && System.currentTimeMillis() < end) {
      Node parent = list.removeFirst();
      List<PentagoMove> moves = parent.state.getAllLegalMoves();
      for (PentagoMove move: moves ) {
            PentagoBoardState state = (PentagoBoardState) parent.state.clone();
            state.processMove(move);
            Node child = new Node(state);
            parent.addChild(child, move);
            list.addLast(child);
      }
    }
    startNode.updateValue();
    System.out.println(System.currentTimeMillis()-t);
    return startNode.getBestMove();
  }

  class Node implements Comparable {
    private PentagoBoardState state;
    private int value;
    private Map<Node, PentagoMove> maps = new HashMap<>();

    @Override
    public int compareTo(Object o) {
      return value;
    }

    public Node(PentagoBoardState state) {
      this.state = state;
    }

    public void addChild(Node n, PentagoMove move) {
      maps.put(n, move);
    }

    public PentagoMove getBestMove() {
      Set<Node> list = maps.keySet();
      return maps.get(Collections.max(list));
    }

    public void updateValue() {
      Set<Node> list = maps.keySet();
      if (list.isEmpty()) {
        value = heuristics(state);
        return;
      } else {
        for (Node n : list) {
          n.updateValue();
        }
      }
      if (color == state.getTurnPlayer()) {
        value = Collections.max(list).value;
      }
      else {
          value = Collections.min(list).value;
      }
    }
  }
}
