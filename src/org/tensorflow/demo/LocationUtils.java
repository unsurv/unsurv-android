package org.tensorflow.demo;

import android.location.Location;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

class LocationUtils {


  static Location getNewLocation(double latitude, double longitude, double metersNorth, double metersEast) {

    double latDiff = metersNorth / 110574;
    double lonDiff = metersEast / longitudeDegreeToMetersRatio(latitude);

    Location updatedLocation = new Location("locationFromLocationAndMeters");

    updatedLocation.setLatitude(latitude + latDiff);
    updatedLocation.setLongitude(longitude + lonDiff);

    return updatedLocation;

  }

  static double longitudeDegreeToMetersRatio(double latitude) {
    // in m
    int earthRadius = 6371000;

    return Math.PI/180*earthRadius*Math.cos(Math.toRadians(latitude));
  }

  static int latitudeDegreeToMetersRatio() {
    // Changes a small amount because earth is not a perfect sphere. Disregarded here
    return 110574;
  }

  static List<Pair<Double, Double>> transferLocationsTo2dCoordinates(List<Location> locations) {
    // Approximates list of Location to 2d coords in relation to first camera. All distances in m.

    // X, Y Values
    List<Pair<Double, Double>> coordinates = new ArrayList<>();

    Location secondLocation;

    //reference location
    Location reference = locations.get(0);
    coordinates.add(new Pair<>(0.0, 0.0));

    // Transfer every element except first into coords.
    for (int i = 1; i < locations.size(); i++) {
      secondLocation = locations.get(i);

      Pair<Double, Double> point = new Pair<>(
              (secondLocation.getLongitude() - reference.getLongitude()) * longitudeDegreeToMetersRatio(reference.getLatitude()), // X
              (secondLocation.getLatitude() - reference.getLatitude()) * latitudeDegreeToMetersRatio() // Y
              );

      coordinates.add(point);

    }

    return coordinates;

  }

  static double sumOfDistancesIn2d(double xValue, double yValue, List<Pair<Double, Double>> points){
    double sumOfDistances = 0;
    double xDiff;
    double yDiff;

    for (int i = 0; i < points.size(); i++){

      xDiff = xValue - points.get(i).first;
      yDiff = yValue - points.get(i).second;

      sumOfDistances += Math.sqrt(xDiff*xDiff + yDiff*yDiff);

    }
    return sumOfDistances;
  }


  static Location approximateCameraPosition(List<Pair<Double, Double>> points, Location reference) {
    // Finds a point where distances to every other point is minimal. Returns a Location created with a reference Location.

    double sumOfDistances;
    double newSumOfDistances;

    double precision = 0.01;

    List<Pair<Integer, Integer>> movementVectors2d = new ArrayList<>();
    movementVectors2d.add(new Pair<>(0, 1)); // N
    movementVectors2d.add(new Pair<>(1, 0)); // E
    movementVectors2d.add(new Pair<>(0, -1)); // S
    movementVectors2d.add(new Pair<>(-1, 0)); // W


    // Value to move around at first step.
    double stepSize = 3;

    // find center of gravity as starting point
    double xTest = 0;
    double yTest = 0;

    for (int i = 0; i < points.size(); i++) {
      xTest += points.get(i).first;
      yTest += points.get(i).second;

    }

    xTest = xTest / points.size(); // center of gravity
    yTest = yTest / points.size();


    sumOfDistances = sumOfDistancesIn2d(xTest, yTest, points);

    while (stepSize > precision){

      for (int j = 0; j < movementVectors2d.size(); j++) {

        // Move point and calc new sum of distances.
        int xMovement = movementVectors2d.get(j).first;
        int yMovement = movementVectors2d.get(j).second;

        double newX = xTest + xMovement;
        double newY = yTest + yMovement;

        newSumOfDistances = sumOfDistancesIn2d(newX, newY, points);

        // Set new point as new start point if sum is smaller
        if (newSumOfDistances < sumOfDistances){
          xTest = newX;
          yTest = newY;
          sumOfDistances = newSumOfDistances;
          break;

        } else {

          // try smaller movements
          stepSize = stepSize / 2;

        }
      }

    }

   return getNewLocation(reference.getLatitude(), reference.getLongitude(), yTest, xTest);

  }


}
