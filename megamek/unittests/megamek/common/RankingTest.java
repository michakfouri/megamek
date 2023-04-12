package megamek.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RankingTest {

    @Test
    public void testUpdateRanking(){
        String playerName = "test1";
        Player player1 = new Player(1, playerName);
        player1.setScore(50);

        playerName = "test2";
        Player player2 = new Player(2, playerName);
        player2.setScore(125);

        playerName = "test3";
        Player player3 = new Player(3, playerName);
        player3.setScore(75);

        Team team1 = new Team(1);
        Team team2 = new Team(2);
        player1.setTeam(team1.getId());
        player2.setTeam(team1.getId());
        player3.setTeam(team2.getId());

        List<Player> playersList = new ArrayList<>();
        playersList.add(player1);
        playersList.add(player2);
        playersList.add(player3);

        Enumeration<Player> playersEnumeration = Collections.enumeration(playersList);
        Ranking ranking = new Ranking(playersEnumeration, player1.getId(), team1.getId());
        ranking.updatePlayersRank();
        assertTrue(player1.getScore() > 50);
        assertTrue(player2.getScore() > 125);
        assertTrue(player3.getScore() < 75);
    }
}
