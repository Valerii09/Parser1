package org.vc.address;

/**
 * Compares text with numeric chunks by their numeric value.
 */
public final class NaturalTextComparator {

    private NaturalTextComparator() {
    }

    public static int compare(String first, String second) {
        String left = first == null ? "" : first;
        String right = second == null ? "" : second;
        int leftIndex = 0;
        int rightIndex = 0;

        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftChar = left.charAt(leftIndex);
            char rightChar = right.charAt(rightIndex);

            if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
                int numberCompare = compareNumberParts(left, leftIndex, right, rightIndex);
                if (numberCompare != 0) {
                    return numberCompare;
                }

                leftIndex = skipDigits(left, leftIndex);
                rightIndex = skipDigits(right, rightIndex);
                continue;
            }

            int charCompare = Character.compare(
                Character.toLowerCase(leftChar),
                Character.toLowerCase(rightChar)
            );
            if (charCompare != 0) {
                return charCompare;
            }

            leftIndex++;
            rightIndex++;
        }

        return Integer.compare(left.length(), right.length());
    }

    private static int compareNumberParts(String first, int firstStart, String second, int secondStart) {
        int firstEnd = skipDigits(first, firstStart);
        int secondEnd = skipDigits(second, secondStart);

        String firstNumber = stripLeadingZeros(first.substring(firstStart, firstEnd));
        String secondNumber = stripLeadingZeros(second.substring(secondStart, secondEnd));

        int lengthCompare = Integer.compare(firstNumber.length(), secondNumber.length());
        if (lengthCompare != 0) {
            return lengthCompare;
        }

        int valueCompare = firstNumber.compareTo(secondNumber);
        if (valueCompare != 0) {
            return valueCompare;
        }

        return Integer.compare(firstEnd - firstStart, secondEnd - secondStart);
    }

    private static int skipDigits(String value, int start) {
        int index = start;

        while (index < value.length() && Character.isDigit(value.charAt(index))) {
            index++;
        }

        return index;
    }

    private static String stripLeadingZeros(String value) {
        String result = value.replaceFirst("^0+", "");

        return result.isEmpty() ? "0" : result;
    }
}
