package megamek.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Ranking {

    private final Enumeration<Player> players;
    private List<Player> winners;
    private List<Player> losers;
    private final int winnerPlayerId;
    private final int winnerTeamId;
    public Ranking(Enumeration<Player> players, int victoryPlayerId, int victoryTeam) {
        this.winnerPlayerId = victoryPlayerId;
        this.winnerTeamId = victoryTeam;
        this.players = players;
        winners = new ArrayList<>();
        losers = new  ArrayList<>();
    }

    public void updatePlayersRank() {
        if (winnerPlayerId == Player.PLAYER_NONE && winnerTeamId == Player.TEAM_NONE) {
            return;
        }

        for (Enumeration<Player> i = players; i.hasMoreElements(); ) {
            final Player player = i.nextElement();
            if (player.getId() == winnerPlayerId) {
                winners.add(player);
            }
            else if (player.getTeam() == winnerTeamId) {
                winners.add(player);
            }
            else {
                losers.add(player);
            }
        }

        giveOrRemovePoints(winners, true);
        giveOrRemovePoints(losers, false);

    }

    private void giveOrRemovePoints(List<Player> team, boolean isWinning) {
        int addScore = 10;
        if(!isWinning){
            addScore = -10;
        }

        for (Player p : team){
            p.setScore(p.getScore() + addScore);
        }
    }
}
