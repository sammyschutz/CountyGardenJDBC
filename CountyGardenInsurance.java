/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package county.garden.insurance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Scanner;

/**
 *
 * @author sammyschutz
 */
public class CountyGardenInsurance {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        connectToDB();
    }

    public static void connectToDB() {
        String user = getString("Enter user id: ");
        String password = getString("Enter password: ");
        boolean connection = false;
        while (!connection) {
            try (Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@edgar1.cse.lehigh.edu:1521:cse241", user, password);
                    PreparedStatement holder = conn.prepareStatement("select user_id from holder where user_id = ?");
                    PreparedStatement agent = conn.prepareStatement("select emp_id from employee where emp_id = ? and title = 'agent'");
                    PreparedStatement adjuster = conn.prepareStatement("select emp_id from employee where emp_id = ? and title = 'adjuster'");
                    CallableStatement cs = conn.prepareCall("{call AGENT_REVENUE (?)}")) {
                System.out.println("Connection successful!");
                connection = true;
                int input = 0;
                while (input != 4) {
                    input = printMainMenu();
                    if (input == 1) {
                        long user_id = holderLogin(conn, holder);
                        int hmenu = 0;
                        while (hmenu != 8) {
                            hmenu = runHolderFunctions(conn, holder, hmenu, user_id);
                        }
                    } else if (input == 2) {
                        long agent_id = agentLogin(conn, agent);
                        int agMenu = 0;
                        while (agMenu != 8) {
                            agMenu = runAgentFunctions(conn, agent, cs, agMenu, agent_id);
                        }
                    } else if (input == 3) {
                        long adj_id = adjusterLogin(conn, adjuster);
                        int adjMenu = 0;
                        while (adjMenu != 7) {
                            adjMenu = runAdjusterFunctions(conn, adjuster, cs, adjMenu, adj_id);
                        }
                    }
                }
                conn.close();
            } catch (SQLException e) {
                System.out.println("Connection Error. Try Again.");
                user = getString("Please enter username: ");
                password = getString("Please enter password: ");
            }
        }
    }

    public static int runHolderFunctions(Connection conn, PreparedStatement holder, int hmenu, long user_id) {
        hmenu = printHolderMenu();
        if (hmenu == 1) {
            addPolicy(conn, holder, user_id);
        } else if (hmenu == 2 && checkForActivePolicies(conn, holder, user_id)) {
            dropPolicy(conn, holder, user_id);
        } else if (hmenu == 3 && checkForActivePolicies(conn, holder, user_id)) {
            payBill(conn, holder, user_id);
        } else if (hmenu == 4 && checkForActivePolicies(conn, holder, user_id)) {
            makeClaim(conn, holder, user_id);
        } else if (hmenu == 5 && checkForActivePolicies(conn, holder, user_id)) {
            addItem(conn, holder, user_id);
        } else if (hmenu == 6 && checkForActivePolicies(conn, holder, user_id)) {
            boolean hasPerson = showUserPersons(conn, holder, user_id);
            boolean hasCar = showUserCars(conn, holder, user_id);
            boolean hasHome = showUserHomes(conn, holder, user_id);
            if (hasPerson || hasCar || hasHome) {
                System.out.println("Here are your various items that you have insured.");
                dropItem(conn, holder, user_id, hasPerson, hasCar, hasHome);
            } else {
                System.out.println("You do not have any items insured so you have nothing to drop.");
            }
        } else if (hmenu == 7) {
            checkForActivePolicies(conn, holder, user_id);
        }
        return hmenu;
    }

    public static int runAgentFunctions(Connection conn, PreparedStatement agent, CallableStatement cs, int agMenu, long agent_id) {
        agMenu = printAgentMenu();
        if (agMenu == 1) {
            addHolder(conn, agent, agent_id);
        } else if (agMenu == 2) {
            getRevenue(conn, cs, agent_id);
        } else if (agMenu == 3 && checkForUsersWithActivePolicies(conn, agent, agent_id)) {
            System.out.println("Here are your holders with active policies.");
            showAgentUsers(conn, agent, agent_id);
            long id = getLong("Enter the user id for the policy which you are adding: ");
            boolean users = checkUserAgentCombo(conn, agent, id, agent_id);
            while (!users) {
                id = getLong("Enter the user id for the policy which you are adding: ");
                users = checkUserAgentCombo(conn, agent, id, agent_id);
            }

            addPolicy(conn, agent, id);
        } else if (agMenu == 4 && checkForUsersWithActivePolicies(conn, agent, agent_id)) {
            System.out.println("Here are your holders with active policies.");
            showAgentUsers(conn, agent, agent_id);
            long id = getLong("Enter the user id for the policy which you are dropping: ");
            boolean users = checkUserAgentCombo(conn, agent, id, agent_id);
            while (!users) {
                id = getLong("Enter the user id for the policy which you are dropping: ");
                users = checkUserAgentCombo(conn, agent, id, agent_id);
            }
            dropPolicy(conn, agent, id);
        } else if (agMenu == 5 && checkForUsersWithActivePolicies(conn, agent, agent_id)) {
            showAgentUsers(conn, agent, agent_id);
            setPremium(conn, agent, agent_id);
        } else if (agMenu == 6 && checkForUsersWithActivePolicies(conn, agent, agent_id)) {
            getRemPrems(conn, agent, agent_id);
        } else if (agMenu == 7 && checkForUsersWithActivePolicies(conn, agent, agent_id)) {
            showAgentUsers(conn, agent, agent_id);
        }
        return agMenu;
    }

    public static int runAdjusterFunctions(Connection conn, PreparedStatement adjuster, CallableStatement cs, int adjMenu, long adj_id) {
        adjMenu = printAdjusterMenu();
        if (adjMenu == 1 && checkClaimsForActivePolicies(conn, adjuster, adj_id)) {
            showAdjusterClaims(conn, adjuster, adj_id);
            setCompany(conn, adjuster, adj_id);
        } else if (adjMenu == 2 && checkClaimsForActivePolicies(conn, adjuster, adj_id)) {
            showAdjusterClaims(conn, adjuster, adj_id);
            serviceClaim(conn, adjuster, adj_id);
        } else if (adjMenu == 3) {
            long id = getLong("Enter the user id for the claim which you are submitting: ");
            boolean activePolicy = checkForActivePolicies(conn, adjuster, id);
            while (!activePolicy) {
                id = getLong("Enter the user id for the claim which you are submitting: ");
                activePolicy = checkForActivePolicies(conn, adjuster, id);
            }
            makeClaim(conn, adjuster, id);
        } else if (adjMenu == 4 && checkClaimsForActivePolicies(conn, adjuster, adj_id)) {
            showAdjusterClaims(conn, adjuster, adj_id);
            adjustRemainDeductible(conn, adjuster, adj_id);
        } else if (adjMenu == 5) {
            avgPaid(conn, cs, adj_id);
        } else if (adjMenu == 6 && checkClaimsForActivePolicies(conn, adjuster, adj_id)) {
            showAdjusterClaims(conn, adjuster, adj_id);
        }
        return adjMenu;
    }

    public static int printMainMenu() {
        int input = getInt("Press \n1 to enter as a holder"
                + "\n2 to enter as an agent"
                + "\n3 to enter as an adjuster"
                + "\n4 to quit");
        return input;
    }

    public static int printHolderMenu() {
        int input = getInt("Press \n1 to add a policy"
                + "\n2 to drop a policy"
                + "\n3 to pay a bill"
                + "\n4 to make a claim"
                + "\n5 to add an item"
                + "\n6 to drop an item"
                + "\n7 to view your active policies"
                + "\n8 to quit back to main menu");
        return input;
    }

    public static long holderLogin(Connection c, PreparedStatement holder) {
        long id = getLong("Enter your user id: ");
        boolean valid = false;
        while (!valid) {
            try {
                holder.setLong(1, id);
                ResultSet rs = holder.executeQuery();
                if (!rs.next()) {
                    System.out.println("Invalid user id. Try again.");
                    id = getLong("Enter your user id: ");
                } else {
                    valid = true;
                    holder = c.prepareStatement("select policy_num from policy where user_id = ? and active = 'active'");
                    holder.setLong(1, id);
                    rs = holder.executeQuery();
                    if (!rs.next()) {
                        System.out.println("Welcome!! You do not have any active policies.");
                        valid = true;
                    } else {
                        System.out.println("Welcome!! Here are your active policies: ");
                        System.out.format("%4s", "Policy Num\n");
                        System.out.println("---------------");
                        do {
                            System.out.println(rs.getLong(1));
                        } while (rs.next());
                    }
                }
            } catch (SQLException e) {
                System.out.println(e);
                id = getLong("Enter your user id: ");
            }
        }
        return id;
    }

    public static boolean checkForActivePolicies(Connection c, PreparedStatement activePol, long user_id) {
        boolean activePolicies = true;
        try {
            activePol = c.prepareStatement("select policy_num from policy where user_id = ? and active = 'active'");
            activePol.setLong(1, user_id);
            ResultSet rs = activePol.executeQuery();
            if (!rs.next()) {
                activePolicies = false;
                System.out.println("Cannot perform this function because this user id does not have any active policies.");
            } else {
                System.out.println("Here are this holder's active policies.");
                System.out.println("Policies");
                System.out.println("--------------");
                do {
                    System.out.println(rs.getLong(1));
                } while (rs.next());
            }

        } catch (SQLException e) {
            System.out.println(e);
        }
        return activePolicies;
    }

    public static boolean checkForRepeatPolicy(Connection c, PreparedStatement repeatPol, long policy) {
        boolean repeat = false;
        try {
            repeatPol = c.prepareStatement("select policy_num from policy where policy_num = ?");
            repeatPol.setLong(1, policy);
            ResultSet rs = repeatPol.executeQuery();
            if (rs.next()) {
                repeat = true;
                System.out.println("This policy number already exists. Try again.");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return repeat;
    }

    public static void addPolicy(Connection c, PreparedStatement addPolicy, long user_id) {
        System.out.println("Note: Premium will be set to null until an agent goes in to adjust it.");
        boolean valid = false;
        while (!valid) {
            long policy = getLong("Enter the policy number which you want to add: ");
            while (checkForRepeatPolicy(c, addPolicy, policy)) {
                policy = getLong("Enter the policy number which you want to add: ");
            }
            try {
                addPolicy = c.prepareStatement("insert into policy (policy_num, user_id, date_created, premium, active) values (?, ?, ?, ?, 'active')");
                addPolicy.setLong(1, policy);
                addPolicy.setLong(2, user_id);
                addPolicy.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
                addPolicy.setNull(4, Types.NULL);
                int rows = addPolicy.executeUpdate();
                if (rows > 0) {
                    System.out.println("Successfully added policy with policy number: " + policy + " for user with id: " + user_id);
                    addPolicy = c.prepareStatement("insert into deductible(policy_num, amount, user_id, amount_remaining) values(?,?,?,?)");
                    addPolicy.setLong(1, policy);
                    addPolicy.setInt(2, 0);
                    addPolicy.setLong(3, user_id);
                    addPolicy.setNull(4, Types.NULL);
                    rows = addPolicy.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Successfully added a deductible row for your policy");
                        valid = true;
                    }
                } else {
                    System.out.println("0 rows updated.");
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean checkPolicyUserCombo(Connection c, PreparedStatement checkPol, long user_id, long policy) {
        boolean belongs = false;
        try {
            checkPol = c.prepareStatement("select policy_num from policy where policy_num = ? and user_id = ? and active = 'active'");
            checkPol.setLong(1, policy);
            checkPol.setLong(2, user_id);
            ResultSet rs = checkPol.executeQuery();
            if (!rs.next()) {
                System.out.println("You have entered a policy that does not exist, is not owned by you, or is not active.");
            } else {
                belongs = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return belongs;
    }

    public static void dropPolicy(Connection c, PreparedStatement dropPolicy, long user_id) {
        boolean valid = false;
        while (!valid) {
            long policy = getLong("Enter the policy number which you want to drop: ");
            try {
                boolean belongs = checkPolicyUserCombo(c, dropPolicy, user_id, policy);
                if (belongs) {
                    dropPolicy = c.prepareStatement("update policy set active = 'inactive' where policy_num = ? and user_id = ?");
                    dropPolicy.setLong(1, policy);
                    dropPolicy.setLong(2, user_id);
                    int rows = dropPolicy.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Successfully dropped your policy with policy number: " + policy);
                    } else {
                        System.out.println("0 rows updated.");
                    }
                    valid = true;
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void payBill(Connection c, PreparedStatement payBill, long user_id) {
        boolean valid = false;
        while (!valid) {
            long policy = getLong("Which one of your policies would you like to pay the bill for?");
            try {
                boolean belongs = checkPolicyUserCombo(c, payBill, user_id, policy);
                if (belongs) {
                    payBill = c.prepareStatement("select prem_remain from policy where policy_num = ? and active = 'active'");
                    payBill.setLong(1, policy);
                    ResultSet rs = payBill.executeQuery();
                    while (rs.next()) {
                        double amount_remain = rs.getDouble(1);
                        System.out.println("Remaining premium balance: $" + amount_remain);
                        valid = true;
                        double amount = getDouble("Enter the amount you want to pay without the dollar sign: ");
                        if (amount > amount_remain) {
                            System.out.println("The amount you entered was greater than your remaining amount. You will only be charged for the amount of the remaining balance.");
                            amount = amount_remain;
                        }
                        System.out.println("Your stored method of payment will now be charged and your premium payment remaining will be reduced.");
                        double newBal = amount_remain - amount;
                        BigDecimal bal = new BigDecimal(newBal).setScale(2, RoundingMode.HALF_DOWN);
                        payBill = c.prepareStatement("update policy set prem_remain = ? where policy_num = " + policy);
                        if (bal.equals(0.00)) {
                            payBill.setNull(1, Types.NULL);
                        } else {
                            payBill.setBigDecimal(1, bal);
                        }
                        int rows = payBill.executeUpdate();
                        if (rows > 0) {
                            System.out.println("Successfully reduced your remaining premium payment to: $" + bal);
                        } else {
                            System.out.println("0 rows updated.");
                        }
                        valid = true;
                    }
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static long selectRandomAdjuster(Connection c, PreparedStatement adjuster) {
        long adjuster_id = 0;
        try {
            adjuster = c.prepareStatement("(SELECT emp_id from\n"
                    + "( SELECT emp_id FROM employee\n"
                    + "where title = 'adjuster'\n"
                    + "ORDER BY dbms_random.value)\n"
                    + "WHERE rownum = 1)");
            ResultSet rs = adjuster.executeQuery();
            while (rs.next()) {
                adjuster_id = rs.getLong(1);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return adjuster_id;
    }

    public static boolean checkForRepeatClaim(Connection c, PreparedStatement repeatClaim, int claim_num) {
        boolean repeat = false;
        try {
            repeatClaim = c.prepareStatement("select claim_num from claim where claim_num = ?");
            repeatClaim.setLong(1, claim_num);
            ResultSet rs = repeatClaim.executeQuery();
            if (rs.next()) {
                repeat = true;
                System.out.println("This claim number already exists. Try again.");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return repeat;
    }

    public static void makeClaim(Connection c, PreparedStatement makeClaim, long user_id) {
        boolean valid = false;
        while (!valid) {
            System.out.println("Note: The amount paid by Counter Garden will be set to null until an adjuster reviews the claim.");
            int claim = getInt("Enter a claim number which you want to add: ");
            while (checkForRepeatClaim(c, makeClaim, claim)) {
                claim = getInt("Enter a claim number which you want to add: ");
            }
            long policy = getLong("Under which policy number should this claim be listed? ");
            boolean belongs = checkPolicyUserCombo(c, makeClaim, user_id, policy);
            while (!belongs) {
                policy = getLong("Under which policy number should this claim be listed? ");
                belongs = checkPolicyUserCombo(c, makeClaim, user_id, policy);
            }
            String description = getString("Type a brief description (40 characters or less) of why you are making this claim: ");
            try {
                makeClaim = c.prepareStatement("insert into claim (claim_num, description, amount_paid, user_id, comp_name, emp_id, policy_num, date_incurred) values (?, ?, ?, ?, ?, ?, ?, ?)");
                makeClaim.setLong(1, claim);
                makeClaim.setString(2, description);
                makeClaim.setNull(3, Types.NULL);
                makeClaim.setLong(4, user_id);
                makeClaim.setNull(5, Types.NULL);
                long adjuster_id = selectRandomAdjuster(c, makeClaim);
                makeClaim.setLong(6, adjuster_id);
                makeClaim.setLong(7, policy);
                makeClaim.setDate(8, java.sql.Date.valueOf(java.time.LocalDate.now()));
                int rows = makeClaim.executeUpdate();
                if (rows == 1) {
                    System.out.println("Successfully added your claim with claim number: " + claim);

                } else {
                    System.out.println("0 rows updated.");
                }
                valid = true;
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean checkSSRepeat(Connection c, PreparedStatement checkSS, String social) {
        boolean repeat = false;
        try {
            checkSS = c.prepareStatement("select user_id from person where social_sec = ?");
            checkSS.setString(1, social);
            ResultSet rs = checkSS.executeQuery();
            if (rs.next()) {
                repeat = true;
                System.out.println("This social security number already exists. Try Again.");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return repeat;
    }

    public static boolean checkCarRepeat(Connection c, PreparedStatement checkCar, String car) {
        boolean repeat = false;
        try {
            checkCar = c.prepareStatement("select user_id from car where VIN = ?");
            checkCar.setString(1, car);
            ResultSet rs = checkCar.executeQuery();
            if (rs.next()) {
                repeat = true;
                System.out.println("This VIN number already exists. Try Again.");
            }
        } catch (SQLException e) {
            System.out.println("Your entered VIN does not match the required length of 17.");
        }
        return repeat;
    }

    public static boolean checkHomeRepeat(Connection c, PreparedStatement checkHome, String home) {
        boolean repeat = false;
        try {
            checkHome = c.prepareStatement("select user_id from home where address = ?");
            checkHome.setString(1, home);
            ResultSet rs = checkHome.executeQuery();
            if (rs.next()) {
                repeat = true;
                System.out.println("This home number already exists. Try Again.");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return repeat;
    }

    public static void addItem(Connection c, PreparedStatement addItem, long user_id) {
        boolean valid = false;
        while (!valid) {
            try {
                String type = getItem("Enter whether the item is a \"person\", \"car\", or \"home\": ");
                String identifier = "";
                if (type.equalsIgnoreCase("person")) {
                    addItem = c.prepareStatement("insert into person values (?, ?, ?)");
                    String regex = "\\d\\d\\d(-)\\d\\d(-)\\d\\d\\d\\d";
                    identifier = getString("What is this person's social security number (XXX-XX-XXXX)? ");

                    while (!identifier.matches(regex)) {
                        identifier = getString("Format did not match. What is this person's social security number (XXX-XX-XXX)? ");
                        while (checkSSRepeat(c, addItem, identifier)) {
                            identifier = getString("What is this person's social security number (XXX-XX-XXXX)? ");
                        }
                    }
                } else if (type.equalsIgnoreCase("car")) {
                    addItem = c.prepareStatement("insert into car values (?, ?, ?)");
                    identifier = getVIN("What is the car's VIN (17 letter/digit combination)? ");
                    while (checkCarRepeat(c, addItem, identifier)) {
                        identifier = getVIN("What is the car's VIN (17 letter/digit combination)? ");
                    }
                } else {
                    addItem = c.prepareStatement("insert into home values (?, ?, ?)");
                    identifier = getString("What is the home's address? ");
                    while (checkHomeRepeat(c, addItem, identifier)) {
                        identifier = getString("What is the home's address? ");
                    }
                }
                long policy = getLong("Under which one of your policies should this item be listed? ");
                boolean belongs = checkPolicyUserCombo(c, addItem, user_id, policy);
                while (!belongs) {
                    policy = getLong("Under which one of your policies should this item be listed? ");
                    belongs = checkPolicyUserCombo(c, addItem, user_id, policy);
                }
                addItem.setString(1, identifier);
                addItem.setLong(2, policy);
                addItem.setLong(3, user_id);
                int rows = addItem.executeUpdate();
                if (rows > 0) {
                    System.out.println("Successfully added your " + type + " to policy number: " + policy);
                } else {
                    System.out.println("0 rows updated.");
                }
                valid = true;
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean checkSSExistence(Connection c, PreparedStatement checkSS, long user_id, String social) {
        boolean belongs = false;
        try {
            checkSS = c.prepareStatement("select user_id from person where user_id = ? and social_sec = ?");
            checkSS.setLong(1, user_id);
            checkSS.setString(2, social);
            ResultSet rs = checkSS.executeQuery();
            if (!rs.next()) {
                System.out.println("You have entered a social security number that does not exist or is not yours.");
            } else {
                belongs = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return belongs;
    }

    public static boolean checkCarExistence(Connection c, PreparedStatement checkCar, long user_id, String car) {
        boolean belongs = false;
        try {
            checkCar = c.prepareStatement("select user_id from car where user_id = ? and VIN = ?");
            checkCar.setLong(1, user_id);
            checkCar.setString(2, car);
            ResultSet rs = checkCar.executeQuery();
            if (!rs.next()) {
                System.out.println("You have entered a car VIN number that does not exist or is not yours.");
            } else {
                belongs = true;
            }
        } catch (SQLException e) {
            System.out.println("Your entered VIN does not match the required length of 17.");
        }
        return belongs;
    }

    public static boolean checkHomeExistence(Connection c, PreparedStatement checkHome, long user_id, String home) {
        boolean belongs = false;
        try {
            checkHome = c.prepareStatement("select user_id from home where user_id = ? and address = ?");
            checkHome.setLong(1, user_id);
            checkHome.setString(2, home);
            ResultSet rs = checkHome.executeQuery();
            if (!rs.next()) {
                System.out.println("You have entered a home address that does not exist or is not yours.");
            } else {
                belongs = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return belongs;
    }

    public static boolean showUserPersons(Connection c, PreparedStatement userItems, long user_id) {
        boolean hasPerson = false;
        try {
            userItems = c.prepareStatement("select social_sec from person where user_id = ?");
            userItems.setLong(1, user_id);
            ResultSet rs = userItems.executeQuery();
            if (!rs.next()) {
                System.out.println("No SSN's with active policies attatched.");
            } else {
                System.out.println("Holder SSN's");
                System.out.println("--------------");
                hasPerson = true;
                do {
                    System.out.println(rs.getString(1));
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return hasPerson;
    }

    public static boolean showUserCars(Connection c, PreparedStatement userItems, long user_id) {
        boolean hasCar = false;
        try {
            userItems = c.prepareStatement("select VIN from car where user_id = ?");
            userItems.setLong(1, user_id);
            ResultSet rs = userItems.executeQuery();
            if (!rs.next()) {
                System.out.println("No cars with active policies attatched.");
            } else {
                System.out.println("Holder VIN's");
                System.out.println("--------------");
                do {
                    System.out.println(rs.getString(1));
                    hasCar = true;
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return hasCar;
    }

    public static boolean showUserHomes(Connection c, PreparedStatement userItems, long user_id) {
        boolean hasHome = false;
        try {
            userItems = c.prepareStatement("select address from home where user_id = ?");
            userItems.setLong(1, user_id);
            ResultSet rs = userItems.executeQuery();
            if (!rs.next()) {
                System.out.println("No homes with active policies attatched.");
            } else {
                System.out.println("Holder Address'");
                System.out.println("--------------");
                do {
                    System.out.println(rs.getString(1));
                    hasHome = true;
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return hasHome;
    }

    public static void dropItem(Connection c, PreparedStatement dropItem, long user_id, boolean hasPerson, boolean hasCar, boolean hasHome) {
        boolean valid = false;
        while (!valid) {
            try {
                String type = getItem("Enter whether the item to be dropped is a \"person\", \"car\", or \"home\": ");
                String identifier = "";
                if (type.equalsIgnoreCase("person") && hasPerson) {
                    boolean belongs = false;
                    dropItem = c.prepareStatement("update person set policy_num = null where social_sec = ?");
                    while (!belongs) {
                        String regex = "\\d\\d\\d(-)\\d\\d(-)\\d\\d\\d\\d";
                        identifier = getString("What is this person's social security number (XXX-XX-XXXX)? ");
                        while (!identifier.matches(regex)) {
                            identifier = getString("Format did not match. What is this person's social security number (XXX-XX-XXX)? ");
                        }
                        belongs = checkSSExistence(c, dropItem, user_id, identifier);
                    }
                } else if (type.equalsIgnoreCase("car") && hasCar) {
                    boolean belongs = false;
                    dropItem = c.prepareStatement("update car set policy_num = null where vin = ?");
                    while (!belongs) {
                        identifier = getVIN("What is the car's VIN? ");
                        belongs = checkCarExistence(c, dropItem, user_id, identifier);
                    }
                } else if (type.equalsIgnoreCase("home") && hasHome) {
                    boolean belongs = false;
                    dropItem = c.prepareStatement("update home set policy_num = null where address = ?");
                    while (!belongs) {
                        identifier = getString("What is the home's address? ");
                        belongs = checkHomeExistence(c, dropItem, user_id, identifier);
                    }
                } else {
                    System.out.println("You entered an item that you do not have insured by an active policy.");
                }
                dropItem.setString(1, identifier);
                int rows = dropItem.executeUpdate();
                if (rows > 0) {
                    System.out.println("Successfully dropped this item from the policy it was ensured by.");
                }
                valid = true;
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static int printAgentMenu() {
        int input = getInt("Press \n1 to add a holder"
                + "\n2 to compute total revenue generated by you"
                + "\n3 to add a policy for one of your holders"
                + "\n4 to drop a policy for one of your holders"
                + "\n5 to set a policy premium for one of your holders"
                + "\n6 to view your holders' remaining premium balances"
                + "\n7 to view your holders and their policies"
                + "\n8 to quit back to main menu");
        return input;
    }

    public static long agentLogin(Connection c, PreparedStatement agLogin) {
        long id = getLong("Enter your employee id: ");
        boolean valid = false;
        while (!valid) {
            try {
                agLogin.setLong(1, id);
                ResultSet rs = agLogin.executeQuery();
                if (!rs.next()) {
                    System.out.println("Invalid employee id. Try Again");
                    id = getLong("Enter your employee id: ");
                } else {
                    valid = true;
                    System.out.println("Welcome!! Here are your customer's names, active policies, and user ids: ");
                    showAgentUsers(c, agLogin, id);
                }
            } catch (SQLException e) {
                System.out.println(e);
                id = getLong("Enter your employee id: ");
            }
        }
        return id;
    }

    public static void showAgentUsers(Connection c, PreparedStatement users, long id) {
        try {
            users = c.prepareStatement("select name, policy_num, user_id from employee natural join holder natural join policy where emp_id = ? and title = 'agent' and active = 'active'");
            users.setLong(1, id);
            ResultSet rs = users.executeQuery();
            if (!rs.next()) {
                System.out.println("You do not have any holders with active policies.");
            } else {
                System.out.format("%4s %17s %21s", "Agent's Customer Name    ", "Policy Num", "User ID\n");
                System.out.println("----------------------------------------------------------------");
                do {
                    System.out.println(String.format("%21s", rs.getString(1)) + String.format("%22d", rs.getLong(2)) + String.format("%21d", rs.getLong(3)));
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static boolean checkForUsersWithActivePolicies(Connection c, PreparedStatement activePol, long agent_id) {
        boolean activePolicies = true;
        try {
            activePol = c.prepareStatement("select policy_num from policy natural join holder where emp_id = ? and active = 'active'");
            activePol.setLong(1, agent_id);
            ResultSet rs = activePol.executeQuery();
            if (!rs.next()) {
                activePolicies = false;
                System.out.println("Cannot perform this function because you do not have any holders with active policies.");
            }

        } catch (SQLException e) {
            System.out.println(e);
        }
        return activePolicies;
    }

    public static boolean checkUserAgentCombo(Connection c, PreparedStatement userAgent, long user_id, long agent_id) {
        boolean users = true;
        try {
            userAgent = c.prepareStatement("select name from holder where emp_id = ? and user_id = ?");
            userAgent.setLong(1, agent_id);
            userAgent.setLong(2, user_id);
            ResultSet rs = userAgent.executeQuery();
            if (!rs.next()) {
                users = false;
                System.out.println("Cannot perform function because this holder does not match any which you are assigned.");
            }

        } catch (SQLException e) {
            System.out.println(e);
        }
        return users;
    }

    public static boolean checkHolder(Connection c, PreparedStatement holder, long user_id) {
        boolean alreadyPresent = false;
        try {
            holder = c.prepareStatement("select user_id from holder where user_id = ?");
            holder.setLong(1, user_id);
            ResultSet rs = holder.executeQuery();
            if (rs.next()) {
                System.out.println("This user id already exists. Try again.");
                alreadyPresent = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return alreadyPresent;
    }

    public static void addHolder(Connection c, PreparedStatement addHolder, long agent_id) {
        System.out.println("Note: Any holder that you add will automatically be listed as one of your customers.");
        boolean valid = false;
        while (!valid) {
            long id = getLong("What user id would you like this person to have?: ");
            boolean alreadyPresent = checkHolder(c, addHolder, id);
            while (alreadyPresent) {
                id = getLong("What user id would you like this person to have?: ");
                alreadyPresent = checkHolder(c, addHolder, id);
            }
            String name = getName("What is his/her name?: ");
            int age = getAge("What is his/her age: ");
            String method_of_payment = getPayment("Enter 'credit', 'debit', or 'direct debit' for his/her method of payment.");
            try {
                addHolder = c.prepareStatement("insert into holder(user_id, name, age, method_of_payment, emp_id) values (?,?,?,?,?)");
                addHolder.setLong(1, id);
                addHolder.setString(2, name);
                addHolder.setInt(3, age);
                addHolder.setString(4, method_of_payment);
                addHolder.setLong(5, agent_id);
                int rows = addHolder.executeUpdate();
                if (rows > 0) {
                    System.out.println("Successfully added holder with user id " + id + " to your list of customers.");
                } else {
                    System.out.println("0 rows updated.");
                }
                valid = true;
                if (method_of_payment.equals("credit")) {
                    addCredit(c, addHolder, id);
                } else if (method_of_payment.equals("debit")) {
                    addDebit(c, addHolder, id);
                } else {
                    addDD(c, addHolder, id);
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean checkRepeatCreditDebit(Connection c, PreparedStatement repeat, long num) {
        boolean alreadyPresent = false;
        try {
            repeat = c.prepareStatement("select cc_number, dc_number from credit_card, debit_card where cc_number = ? or dc_number = ?");
            repeat.setLong(1, num);
            repeat.setLong(2, num);
            ResultSet rs = repeat.executeQuery();
            if (rs.next()) {
                System.out.println("This credit card or debit card number already exists. Try again.");
                alreadyPresent = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return alreadyPresent;
    }

    public static void addCredit(Connection c, PreparedStatement addCredit, long user_id) {
        boolean valid = false;
        while (!valid) {
            try {
                long ccNum = getSpecificLength("Enter their credit card number (XXXXXXXXXXXXXXXX): ", 16, "You have entered a credit card number not equal to 16 digits. Try again");
                while (checkRepeatCreditDebit(c, addCredit, ccNum)) {
                    ccNum = getSpecificLength("Enter their credit card number (XXXXXXXXXXXXXXXX): ", 16, "You have entered a credit card number not equal to 16 digits. Try again");
                }
                int cc_code = getSecurityCode("Enter their security code (XXX): ");
                addCredit = c.prepareStatement("insert into credit_card(cc_number, user_id, ccsecurity_code) values (?,?,?)");
                addCredit.setLong(1, ccNum);
                addCredit.setLong(2, user_id);
                addCredit.setInt(3, cc_code);
                int rows = addCredit.executeUpdate();
                if (rows > 0) {
                    System.out.println("Successfully added credit card for user with id: " + user_id);
                } else {
                    System.out.println("0 rows updated.");
                }
                valid = true;
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void addDebit(Connection c, PreparedStatement addDebit, long user_id) {
        boolean valid = false;
        while (!valid) {
            try {
                long dcNum = getSpecificLength("Enter their debit card number (XXXXXXXXXXXXXXXX): ", 16, "You have entered a debit card number not equal to 16 digits. Try again");
                while (checkRepeatCreditDebit(c, addDebit, dcNum)) {
                    dcNum = getSpecificLength("Enter their debit card number (XXXXXXXXXXXXXXXX): ", 16, "You have entered a debit card number not equal to 16 digits. Try again");
                }
                int dc_code = getSecurityCode("Enter their security code (XXX): ");
                addDebit = c.prepareStatement("insert into debit_card(dc_number, user_id, dcsecurity_code) values (?,?,?)");
                addDebit.setLong(1, dcNum);
                addDebit.setLong(2, user_id);
                addDebit.setInt(3, dc_code);
                int rows = addDebit.executeUpdate();
                valid = true;
                if (rows > 0) {
                    System.out.println("Successfully added debit card for user with id: " + user_id);
                } else {
                    System.out.println("0 rows updated.");
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean checkRepeatDirectDebit(Connection c, PreparedStatement repeat, long ddNum) {
        boolean alreadyPresent = false;
        try {
            repeat = c.prepareStatement("select account_num from direct_debit where account_num = ?");
            repeat.setLong(1, ddNum);
            ResultSet rs = repeat.executeQuery();
            if (rs.next()) {
                System.out.println("This direct debit acount already exists. Try again.");
                alreadyPresent = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return alreadyPresent;
    }

    public static void addDD(Connection c, PreparedStatement addDD, long user_id) {
        boolean valid = false;
        while (!valid) {
            try {
                long ddNum = getSpecificLength("Enter their direct debit account number (XXXXXXXXXXXX): ", 12, "You have entered an account number not equal to 12 digits. Try again");
                while (checkRepeatDirectDebit(c, addDD, ddNum)) {
                    ddNum = getSpecificLength("Enter their direct debit account number (XXXXXXXXXXXX): ", 12, "You have entered an account number not equal to 12 digits. Try again");
                }
                long routing = getSpecificLength("Enter their routing number (XXXXXXXXX): ", 9, "You have entered a routing number not equal to 16 digits. Try again");
                addDD = c.prepareStatement("insert into direct_debit(account_num, routing_num, user_id) values (?,?,?)");
                addDD.setLong(1, ddNum);
                addDD.setLong(2, routing);
                addDD.setLong(3, user_id);
                int rows = addDD.executeUpdate();
                valid = true;
                if (rows > 0) {
                    System.out.println("Successfully added direct debit for user with id: " + user_id);
                } else {
                    System.out.println("0 rows updated.");
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void getRevenue(Connection c, CallableStatement agentRev, long agent_id) {
        try {
            agentRev = c.prepareCall("{call AGENT_REVENUE(?,?)}");
            agentRev.setLong(1, agent_id);
            agentRev.registerOutParameter(2, java.sql.Types.INTEGER);
            agentRev.execute();
            double revenue = agentRev.getDouble(2);
            System.out.println("Revenue Generated: $" + revenue);
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static boolean getNullPremiums(Connection c, PreparedStatement nullPrems, long agent_id) {
        boolean hasNullPrems = false;
        System.out.println("Note: A policy premium needs to be null in order for you to set it.");
        try {
            nullPrems = c.prepareStatement("select policy_num from policy natural join holder where emp_id = ? and premium is null");
            nullPrems.setLong(1, agent_id);
            ResultSet rs = nullPrems.executeQuery();
            if (!rs.next()) {
                System.out.println("None of your holders have null premiums.");
            } else {
                hasNullPrems = true;
                System.out.println("Policies with null premiums");
                System.out.println("----------------------------");
                do {
                    System.out.println(rs.getLong(1));
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return hasNullPrems;
    }

    public static void setPremium(Connection c, PreparedStatement setPrem, long agent_id) {
        boolean valid = false;
        while (!valid) {
            if (!getNullPremiums(c, setPrem, agent_id)) {
                break;
            }
            try {
                long policy = getLong("Enter the policy number of the policy you would like to set the premium for: ");
                setPrem = c.prepareStatement("select policy_num from policy natural join holder where policy_num = ? and emp_id = ? and premium is null");
                setPrem.setLong(1, policy);
                setPrem.setLong(2, agent_id);
                ResultSet rs = setPrem.executeQuery();
                if (!rs.next()) {
                    System.out.println("There was either already a premium for this user or this policy was not owned by one of your assigned holders.");
                } else {
                    setPrem = c.prepareStatement("update policy set premium = ? where policy_num = ?");
                    int premium = getInt("Enter the premium which this policy should have: ");
                    setPrem.setInt(1, premium);
                    setPrem.setLong(2, policy);
                    int rows = setPrem.executeUpdate();
                    valid = true;
                    if (rows > 0) {
                        System.out.println("Successfully set the premium of policy: " + policy + " to be: " + premium);
                    } else {
                        System.out.println("0 rows updated.");
                    }
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void getRemPrems(Connection c, PreparedStatement getRemPrem, long agent_id) {
        try {
            getRemPrem = c.prepareStatement("select name, prem_remain as premium from policy natural join holder where emp_id = ? and active = 'active' and prem_remain is not null");
            getRemPrem.setLong(1, agent_id);
            ResultSet rs = getRemPrem.executeQuery();
            if (!rs.next()) {
                System.out.println("None of your holders have remaining premium balances.");
            } else {
                System.out.format("%24s %20s", "Name    ", "Premium Remaining\n");
                System.out.println("----------------------------------------------");
                do {
                    System.out.println(String.format("%20s", rs.getString(1)) + String.format("%24.2f", rs.getDouble(2)));
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static int printAdjusterMenu() {
        int input = getInt("Press \n1 to set the outsourcing company of your claims"
                + "\n2 to service one of your claims"
                + "\n3 to submit a claim for a holder"
                + "\n4 to adjust the remaining deductible on a claim you are working on"
                + "\n5 to get the average amount of all the payments by you"
                + "\n6 to view your active "
                + "\n7 to quit back to main menu");
        return input;
    }

    public static long adjusterLogin(Connection c, PreparedStatement adjLogin) {
        long id = getLong("Enter your employee id: ");
        boolean valid = false;
        while (!valid) {
            try {
                adjLogin.setLong(1, id);
                ResultSet rs = adjLogin.executeQuery();
                if (!rs.next()) {
                    System.out.println("Invalid employee id. Try Again");
                    id = getLong("Enter your employee id: ");
                } else {
                    valid = true;
                    System.out.println("Welcome!! Here are the active claims you have, the date they were incurred, and the active policy attached to the claim: ");
                    showAdjusterClaims(c, adjLogin, id);
                }
            } catch (SQLException e) {
                System.out.println(e);
                id = getLong("Enter your employee id: ");
            }
        }
        return id;
    }

    public static void showAdjusterClaims(Connection c, PreparedStatement claims, long id) {
        try {
            claims = c.prepareStatement("select claim_num, date_incurred, policy_num from employee natural join claim natural join policy where emp_id = ? and title = 'adjuster' and active = 'active' and amount_paid is null");
            claims.setLong(1, id);
            ResultSet rs = claims.executeQuery();
            if (!rs.next()) {
                System.out.println("You do not have any claims with active policies attatched to them.");
            } else {
                System.out.format("%4s %20s %20s", "Claim Num", "Date Incurred", "Policy Num\n");
                System.out.println("-------------------------------------------------------");
                do {
                    System.out.println(String.format("%9d", rs.getLong(1)) + String.format("%21s", rs.getDate(2))
                            + String.format("%21d", rs.getLong(3)));
                } while (rs.next());
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static boolean checkClaimsForActivePolicies(Connection c, PreparedStatement activePol, long adj_id) {
        boolean activePolicies = true;
        try {
            activePol = c.prepareStatement("select policy_num from policy natural join claim where emp_id = ? and active = 'active' and amount_paid is null");
            activePol.setLong(1, adj_id);
            ResultSet rs = activePol.executeQuery();
            if (!rs.next()) {
                activePolicies = false;
                System.out.println("Cannot perform this function because you do not have any claims that have not been paid or with active policies attatched to them.");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return activePolicies;
    }

    public static void setCompany(Connection c, PreparedStatement setComp, long adj_id) {
        System.out.println("Note: A company name needs to be null in order for you to set it.");
        boolean valid = false;
        while (!valid) {
            try {
                int claim = getInt("Enter the claim number of the claim you would like to set the company for: ");
                setComp = c.prepareStatement("select claim_num from claim natural join policy where claim_num = ? and emp_id = ? and comp_name is null and active = 'active' and amount_paid is null");
                setComp.setInt(1, claim);
                setComp.setLong(2, adj_id);
                ResultSet rs = setComp.executeQuery();
                if (!rs.next()) {
                    System.out.println("There was either already a company for this claim, this claim was not one of your assigned ones, or the claim had already been paid.");
                    break;
                } else {
                    setComp = c.prepareStatement("update claim set comp_name = ? where claim_num = ?");
                    String comp = getCompany(c, setComp, "Enter the company that this claim will be outsourced to: ");
                    setComp.setString(1, comp);
                    setComp.setLong(2, claim);
                    setComp.executeUpdate();
                    valid = true;
                    System.out.println("Successfully set the outsource company of claim: " + claim + " to be: " + comp);
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void serviceClaim(Connection c, PreparedStatement serviceClaim, long adj_id) {
        System.out.println("Note: A claim needs to have a null amount paid in order for you to set it.");
        boolean valid = false;
        while (!valid) {
            try {
                int claim = getInt("Enter the claim number of the claim you would like to service: ");
                serviceClaim = c.prepareStatement("select claim_num from claim natural join policy where claim_num = ? and emp_id = ? and amount_paid is null and active = 'active'");
                serviceClaim.setInt(1, claim);
                serviceClaim.setLong(2, adj_id);
                ResultSet rs = serviceClaim.executeQuery();
                if (!rs.next()) {
                    System.out.println("There was either already a paid amount for this claim, this claim was not one of your assigned ones, or the policy was not active.");
                    break;
                } else {
                    serviceClaim = c.prepareStatement("update claim set amount_paid = ? where claim_num = ?");
                    double amount = getDouble("Enter the amount County Garden is paying for this claim: ");
                    serviceClaim.setDouble(1, amount);
                    serviceClaim.setInt(2, claim);
                    int rows = serviceClaim.executeUpdate();
                    valid = true;
                    if (rows > 0) {
                        System.out.println("Successfully set the amount paid of claim: " + claim + " to be: $" + amount);
                    } else {
                        System.out.println("O rows updated");
                    }
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean checkAdjusterClaimCombo(Connection c, PreparedStatement checkClaim, long adj_id, long policy) {
        boolean belongs = false;
        try {
            checkClaim = c.prepareStatement("select policy_num from claim natural join policy where policy_num = ? and emp_id = ? and active = 'active'");
            checkClaim.setLong(1, policy);
            checkClaim.setLong(2, adj_id);
            ResultSet rs = checkClaim.executeQuery();
            if (!rs.next()) {
                System.out.println("You have entered a policy that does not exist, is not being serviced by you.");
            } else {
                belongs = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return belongs;
    }

    public static void adjustRemainDeductible(Connection c, PreparedStatement payBill, long adj_id) {
        boolean valid = false;
        while (!valid) {
            long policy = getLong("Which policy would you like to adjust the remaining deductible for? ");
            try {
                boolean belongs = checkAdjusterClaimCombo(c, payBill, adj_id, policy);
                if (belongs) {
                    payBill = c.prepareStatement("select amount_remaining from deductible where policy_num = ?");
                    payBill.setLong(1, policy);
                    ResultSet rs = payBill.executeQuery();
                    if (!rs.next()) {
                        System.out.println("There was no remaining deductible to be met for this policy.");
                        break;
                    } else {
                        double amount_remain = rs.getDouble(1);
                        System.out.println("Remaining deductible to be met: " + amount_remain);
                        valid = true;
                        double amount = getDouble("Enter the amount that should be deducted: ");
                        while (amount < 0) {
                            amount = getDouble("You entered an amount less than $0. Enter the amount that should be deducted: ");
                        }
                        if (amount > amount_remain) {
                            System.out.println("The amount you entered was greater than the remaining amount. Only the remaining balance will be deducted.");
                            amount = amount_remain;
                        }
                        double newBal = amount_remain - amount;
                        BigDecimal bal = new BigDecimal(newBal).setScale(2, RoundingMode.HALF_DOWN);
                        payBill = c.prepareStatement("update deductible set amount_remaining = ? where policy_num = " + policy);
                        if (bal.equals(0.00)) {
                            payBill.setNull(1, Types.NULL);
                        } else {
                            payBill.setBigDecimal(1, bal);
                        }
                        int rows = payBill.executeUpdate();
                        if (rows > 0) {
                            System.out.println("Successfully reduced the remaining deductible to be met to: $" + bal);
                        } else {
                            System.out.println("0 rows updated.");
                        }
                        valid = true;
                    }
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void avgPaid(Connection c, CallableStatement avgPaid, long adj_id) {
        try {
            avgPaid = c.prepareCall("{call AVG_AMOUNT_PAID(?,?)}");
            avgPaid.setLong(1, adj_id);
            avgPaid.registerOutParameter(2, java.sql.Types.DOUBLE);
            avgPaid.execute();
            double amount = avgPaid.getDouble(2);
            System.out.println("Avg amount paid out by you: " + amount);
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static int getInt(String prompt) {
        int input = 0;
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextInt()) {
                input = userInput.nextInt();
                valid = true;
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static int getAge(String prompt) {
        int input = 0;
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextInt()) {
                input = userInput.nextInt();
                if (input <= 0 || input >= 100) {
                    System.out.println("You enter an invalid format for age. An age cannot be more than 2 digits.");
                } else {
                    valid = true;
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static String getString(String prompt) {
        String input = "";
        boolean valid = false;
        while (!valid) {
            System.out.println(prompt);
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLine()) {
                input = userInput.nextLine();
                if (input.length() < 40 && !input.contains("update") && !input.contains("insert") && !input.contains("drop")) {
                    valid = true;
                } else {
                    System.out.println("Suspicious activity detected. Do not enter data that contains \'update\', \'insert\', or \'drop\' or that is more than 40 characters. Try again.");
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static String getCompany(Connection c, PreparedStatement company, String prompt) {
        String input = "";
        boolean valid = false;
        while (!valid) {
            System.out.println(prompt);
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLine()) {
                input = userInput.nextLine();
                try {
                    company = c.prepareStatement("select comp_name from company where comp_name = ?");
                    company.setString(1, input);
                    ResultSet rs = company.executeQuery();
                    if (!rs.next()) {
                        System.out.println("You entered a company that is not among the list of valid ones.");
                    } else {
                        valid = true;
                    }
                } catch (SQLException e) {
                    System.out.println(e);
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static String getName(String prompt) {
        String input = "";
        boolean valid = false;
        while (!valid) {
            System.out.println(prompt);
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLine()) {
                input = userInput.nextLine();
                if (input.length() > 20) {
                    System.out.println("Names cannot be more than 20 characters. Try Again." + prompt);
                } else if (input.contains("update") || input.contains("insert") || input.contains("drop")) {
                    System.out.println("Suspicious activity detected. Do not enter data that contains \'update\', \'insert\', or \'drop\' Try again.");
                } else {
                    valid = true;
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static String getVIN(String prompt) {
        String input = "";
        boolean valid = false;
        while (!valid) {
            System.out.println(prompt);
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLine()) {
                input = userInput.nextLine();
                if (input.length() != 17) {
                    System.out.println("You have entered a VIN whose length is not equal to 17. Try again.");
                } else if (input.contains("update") || input.contains("insert") || input.contains("drop")) {
                    System.out.println("Suspicious activity detected. Do not enter data that contains \'update\', \'insert\', or \'drop\' Try again.");
                } else {
                    valid = true;
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static String getItem(String prompt) {
        String input = "";
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLine()) {
                input = userInput.nextLine();
                if (input.equalsIgnoreCase("person")) {
                    valid = true;
                } else if (input.equalsIgnoreCase("car")) {
                    valid = true;
                } else if (input.equalsIgnoreCase("home")) {
                    valid = true;
                } else {
                    System.out.println("You did not enter a proper type of item. Please try again.");
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static String getPayment(String prompt) {
        String input = "";
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLine()) {
                input = userInput.nextLine();
                if (input.equalsIgnoreCase("credit")) {
                    valid = true;
                } else if (input.equalsIgnoreCase("debit")) {
                    valid = true;
                } else if (input.equalsIgnoreCase("direct debit")) {
                    valid = true;
                } else {
                    System.out.println("You did not enter a proper method of payment. Please try again.");
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static long getLong(String prompt) {
        long input = 0;
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLong()) {
                input = userInput.nextLong();
                valid = true;
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static long getSpecificLength(String prompt, int length, String error) {
        long input = 0;
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextLong()) {
                input = userInput.nextLong();
                if (Long.toString(input).length() != length) {
                    System.out.println(error);
                } else {
                    valid = true;
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static int getSecurityCode(String prompt) {
        int input = 0;
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextInt()) {
                input = userInput.nextInt();
                if (Integer.toString(input).length() != 3) {
                    System.out.println("You enter an invalid format for security code. It needs to be 3 digits.");
                } else {
                    valid = true;
                }
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }

    public static double getDouble(String prompt) {
        double input = 0;
        boolean valid = false;
        System.out.println(prompt);
        while (!valid) {
            Scanner userInput = new Scanner(System.in);
            if (userInput.hasNextDouble()) {
                input = userInput.nextDouble();
                valid = true;
            } else {
                System.out.println("Invalid type. " + prompt);
            }
        }
        return input;
    }
}
