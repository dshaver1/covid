function updateBarChart(svg, data, xScale, xCallback, yScale, yCallback, clazz, color, highlightColor) {
    var selectedData = svg.selectAll("." + clazz).data(data);
    var enterData = selectedData.enter();

    enterData.append("rect")
        .style("shape-rendering", "crispEdges")
        .attr("class", clazz)
        .attr("x", function (d) {
            return xScale(xCallback(d));
        })
        .attr("y", function (d) {
            if (yCallback(d) > 0) {
                return yScale(yCallback(d));
            } else {
                return yScale(0);
            }
        })
        .attr("height", function (d) {
            return Math.abs(yScale(yCallback(d)) - yScale(0));
        })
        .attr("width", xScale.bandwidth())
        .style("fill", function (d) {
            if (yCallback(d) > 0) {
                return color;
            }

            return highlightColor;
        });

    selectedData.transition().duration(100)
        .attr("y", function (d) {
            if (yCallback(d) > 0) {
                return yScale(yCallback(d));
            } else {
                return yScale(0);
            }
        })
        .attr("height", function (d) {
            return Math.abs(yScale(yCallback(d)) - yScale(0));
        })
        .style("fill", function (d) {
            if (yCallback(d) > 0) {
                return color;
            }

            return highlightColor;
        });

    selectedData.exit().remove();

    // Ensure notable dates lines are on top!
    svg.selectAll("." + clazz).moveToFront();
    svg.selectAll("line").moveToFront();
    svg.selectAll('.mouseoverclazz').moveToFront();
}

function updateLineChart(svg, data, xScale, xCallback, yScale, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded) {
    var selectedData = svg.selectAll("." + clazz).data([data], function (d) {
        return xCallback(d);
    });

    // Initial Data
    selectedData.enter()
        .append("path")
        .attr("class", clazz)
        .attr("d", d3.line()
            .x(function (d) {
                return getLineX(xCallback(d), xScale, isBanded);
            })
            .y(function (d) {
                return getLineY(yCallback(d), yScale);
            })
            // Don't draw the line if it's in the preliminary region.
            .defined(function (d) {
                return isPointDefined(yCallback, d, xScale, prelimRegionStart);
            }))
        .attr("fill", "none")
        .attr("stroke", color)
        .attr("stroke-width", 1);

    // Updated Data
    selectedData
        .merge(selectedData)
        .transition()
        .duration(100)
        .attr("d", d3.line()
            .x(function (d) {
                return getLineX(xCallback(d), xScale, isBanded);
            })
            .y(function (d) {
                return getLineY(yCallback(d), yScale);
            })
            // Don't draw the line if it's in the preliminary region.
            .defined(function (d) {
                return isPointDefined(yCallback, d, xScale, prelimRegionStart);
            }))

    var circleClass = clazz + '-circle';
    var isVisible = "visible" === svg.selectAll('.' + clazz).style("visibility");
    var selectedCircles = svg.selectAll('.' + circleClass).data(data);

    // Initial data
    selectedCircles.enter()
        .append('circle')
        .attr('class', circleClass + " " + clazz)
        .attr('cx', function (d) {
            return getLineX(xCallback(d), xScale, isBanded);
        })
        .attr('cy', function (d) {
            return getLineY(yCallback(d), yScale);
        })
        .attr("stroke", highlightColor)
        .attr("fill", color)
        .attr("opacity", function (d) {
            return getLineOpacity(yCallback, d, xScale, prelimRegionStart);
        })
        .attr('r', 2)
        .style("visibility", function (d) {
            if (isVisible) {
                return "visible";
            }

            return "hidden";
        });

    // Updated data
    selectedCircles
        .merge(selectedCircles)
        .transition()
        .duration(100)
        .attr("opacity", function (d) {
            return getLineOpacity(yCallback, d, xScale, prelimRegionStart);
        })
        .attr('cx', function (d) {
            return getLineX(xCallback(d), xScale, isBanded);
        })
        .attr('cy', function (d) {
            return getLineY(yCallback(d), yScale);
        })

    // Removed data
    selectedCircles.exit().transition().duration(100).style("opacity", 0).remove();

    // Ensure notable dates lines are on top!
    svg.selectAll("." + clazz).moveToFront();
    svg.selectAll("line").moveToFront();
    svg.selectAll('.' + circleClass).moveToFront();
    svg.selectAll('.mouseoverclazz').moveToFront();
}

function updateFloatingPoints(svg, data, xScale, xCallback, yScale, yCallback, clazz, color) {
    let selectedData = svg.selectAll("." + clazz).data(data);

    let triangle = d3.symbol()
        .type(d3.symbolDiamond)
        .size(20);

    let existing = svg.selectAll('.' + clazz);
    let isVisible = true;
    if (existing) {
        try {
            isVisible = "visible" === existing.style("visibility");
        } catch (err) {
            isVisible = true;
        }
    }

    selectedData.enter().append("path")
        .attr("class", clazz)
        .attr("d", triangle)
        .attr("stroke", color)
        .attr("fill", color)
        .attr("opacity", function (d) {
            return getLineOpacity(yCallback, d, xScale, null);
        })
        .attr("transform", function (d) {
            return "translate(" + getLineX(xCallback(d), xScale, true) + "," + getLineY(yCallback(d), yScale) + ")";
        })
        .style("visibility", function (d) {
            if (isVisible) {
                return "visible";
            }

            return "hidden";
        });

    selectedData.exit().transition().duration(100).style("opacity", 0).remove();
}

function getLineX(d, xScale, isBanded) {

    if (isBanded) {
        return xScale(d) + (xScale.bandwidth() / 2);
    }

    return xScale(d);
}

/**
 * Used to determine if a data point should be shown on the graph. NaN's should not be shown, and neither should points
 * after the given 'preliminary' region start date.
 *
 * @param dataCallback The callback to get the Y value
 * @param dataPoint The data point object which contains all relevant information for the given point.
 * @param prelimRegionStart The start of the preliminary region. Points after this should not be shown.
 * @returns true if the point should be shown on the graph, false if the point should not be shown.
 */
function isPointDefined(dataCallback, dataPoint, xScale, prelimRegionStart) {
    if (isNaN(dataCallback(dataPoint))) {
        return false;
    }

    if (prelimRegionStart) {
        return xScale(dataPoint.label) < xScale(prelimRegionStart);
    }

    return true;
}

/**
 * Convenience function to check for NaN's.
 */
function getLineY(d, yScale) {
    if (!isNaN(d)) {
        return yScale(d);
    }

    return 0;
}

/**
 * Convenience function to set opacity to 0 for NaN, and 0.5 for preliminary region.
 */
function getLineOpacity(dataCallback, dataPoint, xScale, prelimRegionStart) {
    if (isNaN(dataCallback(dataPoint))) {
        return "0";
    }

    if (prelimRegionStart && xScale(dataPoint.label) >= xScale(prelimRegionStart)) {
        return "0.5";
    }

    return "1";
}

function drawMouseOverRects(svg, xScale, xCallback, data, height) {
    var selectedData = svg.selectAll(".mouseoverclazz").data(data);
    var enterData = selectedData.enter();

    enterData.append("rect")
        .attr("class", "mouseoverclazz")
        .attr("x", d => xScale(xCallback(d)))
        .attr("y", function (d) {
            return 0;
        })
        .attr("height", function (d) {
            return height;
        })
        .attr("width", xScale.bandwidth())
        .style("fill", "#999")
        .attr("opacity", "0")
        .on("mouseover", function (d, i) {
            handleDeathMouseOver(d, i, d3.select(this), "#fff")
        })
        .on("mouseout", function (d, i) {
            handleDeathMouseOut(d, i, d3.select(this), "#999")
        });

    selectedData.exit().remove();
}

function draw14DayWindow(svg, xScale, xCallback, offsetDate, offset, height) {
    let selectedData = svg.selectAll(".prelim-region").data([offsetDate]);
    let enterData = selectedData.enter();

    // draw initial region
    enterData
        .append("rect")
        .style("shape-rendering", "crispEdges")
        .attr("class", "prelim-region")
        .attr("x", function (d) {
            return xScale(d);
        })
        .attr("y", function (d) {
            return 0;
        })
        .attr("height", function (d) {
            return height;
        })
        .attr("width", xScale.bandwidth() * (offset + 1))
        .style("fill", "#c6d1ff")
        .attr("opacity", "0.05");

    // update region location
    selectedData
        .merge(selectedData)
        .transition()
        .duration(100)
        .attr("x", function (d) {
            return xScale(d);
        });


    let boundaryLineSelectedData = svg.selectAll(".prelim-boundary-line").data([offsetDate]);
    let boundaryLineEnterData = boundaryLineSelectedData.enter();

    // draw initial boundary line
    boundaryLineEnterData
        .append("line")
        .attr("class", "prelim-boundary-line")
        .style("stroke", "#7ba4c9")
        .style("shape-rendering", "crispEdges")
        .style("stroke-width", 1)
        .attr("x1", function (d) {
            return xScale(d);
        })
        .attr("y1", 0)
        .attr("x2", function (d) {
            return xScale(d);
        })
        .attr("y2", height);

    // update boundary line location
    boundaryLineSelectedData
        .merge(boundaryLineSelectedData)
        .transition()
        .duration(100)
        .attr("x1", function (d) {
            return xScale(d);
        })
        .attr("x2", function (d) {
            return xScale(d);
        });

    // draw initial text
    let textSelectedData = svg.selectAll(".prelim-text").data([{date: offsetDate, label: "Preliminary Data"}]);
    let textEnterData = textSelectedData.enter();

    textEnterData
        .append("text")
        .attr("class", "prelim-text")
        .attr("x", function (d) {
            return -80;
        })
        .attr("y", function (d, i) {
            return xScale(d.date) + xScale.bandwidth();
        })
        .text(function (d) {
            return xCallback(d);
        })
        .attr("text-anchor", "top")
        .style("alignment-baseline", "top")
        // .attr("dx", "-.8em")
        // .attr("dy", "-.55em")
        .attr("transform", "rotate(-90)")

    textSelectedData
        .merge(textSelectedData)
        .transition()
        .duration(100)
        .attr("y", function (d) {
            return xScale(d.date) + xScale.bandwidth();
        });
}

function applyDateOffset(date, offset) {
    let dateObj = new Date(date);
    dateObj.setDate(dateObj.getDate() - offset);
    return getFormattedDate(dateObj);
}

function getFormattedDate(date) {
    let year = date.getFullYear();
    let month = (1 + date.getMonth()).toString().padStart(2, '0');
    let day = date.getDate().toString().padStart(2, '0');

    return year + '-' + month + '-' + day;
}

function getLastElement(data) {
    let keySet = Object.keys(data);
    return data[keySet[keySet.length - 1]];
}

/**
 * Parses the provided incoming csv files into a more useable JSON document, indexed by report ID (ex: 2020-05-27T09:00:03)
 *
 * @param summaryData The summary csv
 * @param caseData The case csv
 * @param deathData The death csv
 * @param caseDeltaData The case delta csv
 * @param deathDeltaData The death delta csv
 * @param caseProjectionData The case projection csv
 * @param movingAvgData The moving averages csv
 * @returns [{
 *     cases: 2,
 *     deaths: 0,
 *     label: "2020-02-17"
 * },{
 *     cases: 36,
 *     deaths: 0,
 *     label: "2020-05-27"
 * }]
 */
function parseChartData(summaryData, caseData, deathData, caseDeltaData, deathDeltaData, caseProjectionData, movingAvgData) {
    let tempChartData = [];

    let reportedCasesData = []
    summaryData.forEach(function (d) {
        reportedCasesData[d.reportDate] = d.confirmedCasesVm;
    });

    let reportedDeathsData = []
    summaryData.forEach(function (d) {
        reportedDeathsData[d.reportDate] = d.deathsVm;
    });

    for (let i = 0; i < summaryData.length; i++) {
        let currentSummaryData = summaryData[i];
        let currentCaseData = caseData[i];
        let currentDeathData = deathData[i];
        let currentCaseDeltaData = caseDeltaData[i];
        let currentDeathDeltaData = deathDeltaData[i];
        let currentCaseProjectionData = caseProjectionData[i];
        let currentMovingAvgData = movingAvgData[i];
        let currentReportDates = Object.keys(currentCaseData).filter(function (d) {
            return d !== 'id'
        });
        let currentTimeseries = tempChartData[currentSummaryData.id];
        if (!currentTimeseries) {
            currentTimeseries = [];
            tempChartData[currentSummaryData.id] = currentTimeseries;
        }
        currentReportDates
            .filter(function (d) {
                return !!currentCaseData[d];
            })
            .forEach(function (d) {
                currentTimeseries.push({
                    label: d,
                    cases: +currentCaseData[d],
                    deaths: +currentDeathData[d],
                    casesDelta: +currentCaseDeltaData[d],
                    deathsDelta: +currentDeathDeltaData[d],
                    casesProjection: +currentCaseProjectionData[d],
                    movingAvg: +currentMovingAvgData[d],
                    reportedCases: +reportedCasesData[d],
                    reportedDeaths: +reportedDeathsData[d]
                });
            });
    }

    return tempChartData;
}

function parseCorrelationData(summaryData, caseData, deathData, deathOffset) {
    let tempChartData = [];

    let reportedCasesData = []
    summaryData.forEach(function (d) {
        reportedCasesData[d.reportDate] = d.confirmedCasesVm;
    });

    let reportedDeathsData = []
    summaryData.forEach(function (d) {
        reportedDeathsData[d.reportDate] = d.deathsVm;
    });

    let dateIndexLookup = {};
    let indexDateLookup = {};
    let allReportDates = Object.keys(deathData[deathData.length - 1]).filter(d => d !== 'id');
    for (let i = 0; i < allReportDates.length; i++) {
        let currentDate = allReportDates[i];
        dateIndexLookup[currentDate] = i;
        indexDateLookup[i] = currentDate;
    }

    for (let i = 0; i < summaryData.length; i++) {
        let currentSummaryData = summaryData[i];
        let currentCaseData = caseData[i];
        let currentDeathData = deathData[i];
        let currentReportDates = Object.keys(currentCaseData).filter(function (d) {
            return d !== 'id'
        });
        let currentTimeseries = tempChartData[currentSummaryData.id];
        if (!currentTimeseries) {
            currentTimeseries = [];
            tempChartData[currentSummaryData.id] = currentTimeseries;
        }
        currentReportDates
            .filter(function (d) {
                return !!currentCaseData[d];
            })
            .forEach(function (d) {
                let deathOffsetDate = indexDateLookup[dateIndexLookup[d] + deathOffset];
                if (deathOffsetDate && currentDeathData[deathOffsetDate]) {
                    currentTimeseries.push({
                        label: d,
                        cases: +currentCaseData[d],
                        deaths: +currentDeathData[deathOffsetDate],
                        casesAvg: calcMovingAverage(indexDateLookup, dateIndexLookup, currentCaseData, d, 7),
                        deathsAvg: calcMovingAverage(indexDateLookup, dateIndexLookup, currentDeathData, deathOffsetDate, 7),
                        reportedCases: +reportedCasesData[d] || 0,
                        reportedDeaths: +reportedDeathsData[deathOffsetDate] || 0,
                        reportedCasesAvg: calcMovingAverage(indexDateLookup, dateIndexLookup, reportedCasesData, d, 7),
                        reportedDeathsAvg: calcMovingAverage(indexDateLookup, dateIndexLookup, reportedDeathsData, deathOffsetDate, 7),
                    });
                }
            });
    }

    return tempChartData;
}

function calcMovingAverage(indexDateLookup, dateIndexLookup, data, currentDate, window) {
    let currentDateIdx = dateIndexLookup[currentDate];

    let sum = 0;
    for (let i = 0; i < 7; i++) {
        sum += +data[indexDateLookup[currentDateIdx - i]] || 0;
    }

    return sum / window;
}

function make_y_gridlines(yScale) {
    return d3.axisLeft(yScale).tickValues([0, 100, 200, 300, 400, 500, 600, 700, 800, 900])
}

function drawAxisLines(svg, yScale) {
    svg.selectAll(".grid").remove();
    svg.append("g")
        .attr("class", "grid")
        .call(make_y_gridlines(yScale)
            .tickSize(-width)
            .tickFormat("")
        );
}

function createXAxisTickValues(data) {
    let epicurve = getLastElement(data);
    let xAxisTickValues = [];

    for (let i = 0; i < epicurve.length; i += 7) {
        xAxisTickValues.push(epicurve[i].label);
    }

    return xAxisTickValues;
}

function createAxis(data, parent, xAxis, yAxis, height, yLabel) {
    parent.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis.tickValues(createXAxisTickValues(data)))
        .selectAll("text")
        .style("text-anchor", "end")
        .attr("dx", "-.8em")
        .attr("dy", "-.55em")
        .attr("transform", "rotate(-90)");

    parent.append("g")
        .attr("class", "y axis")
        .call(yAxis)
        .append("text")
        .attr("y", 5)
        .attr("x", 5)
        //.attr("dy", ".71em")
        .style("text-anchor", "end")
        .text(yLabel)
        .attr("transform", "translate(5,5)rotate(-90)");
}

function getYScale(data) {
    if (deltaEnabled) {
        return 300;
    }

    return 50 + d3.max(data, function (d) {
        return d.reportedCases;
    })
}

function createSlider(data) {
    let count = 0;

    for (let value of Object.values(data)) {
        if (value.length > 0) {
            count++
        }
    }

    count = (count - 1) * -1;

    let tickValueArray = [];

    for (let current = 0; current > count; current = current - 7) {
        tickValueArray.push(current);
    }

    let sliderTime = d3
        .sliderBottom()
        //.min(-1 * (timeData.length - 1))
        //.max(0)
        .domain([count, 0])
        .marks([count, 0])
        .step(1)
        //.ticks(5)
        .width(350)
        .tickValues(tickValueArray);

    var gTime = d3
        .select('div#slider-time')
        .append('svg')
        .attr('width', 400)
        .attr('height', 100)
        .append('g')
        .attr('transform', 'translate(30,30)');

    gTime.call(sliderTime);

    return sliderTime;
}

/**
 * Gets the value from the provided array as if it were indexed from the end. So 0 gets the last element, and -data.length-1 gets the first element.
 * -1 Gets the next to the last element.
 */
function getReverseIdxValue(data, idx) {
    return data[data.length - 1 + idx];
}

// Create Event Handlers for mouse
function handleDeathMouseOver(d, i, d3This, color) {
    d3This
        .transition()
        .duration(50)
        .style("fill", color)
        .attr("opacity", "0.2");
    tip.show(d, i);
}

function handleDeathMouseOut(d, i, d3This, color) {
    d3This
        .transition()
        .duration(200)
        .style("fill", color)
        .attr("opacity", "0");
    tip.hide(d, i);
}

function createTooltips(svg, value) {
    tip = d3.tip().attr('class', 'd3-tip').direction('e').offset([0, 5])
        .html(function (d) {
            var content = "<span style='margin-left: 2.5px;'><b>" + d.label + "</b></span><br>";
            content += `
                    <table style="margin-top: 2.5px;">
                            <tr><td>Reported Cases: </td><td style="text-align: right">` + getReportedCases(d) + `</td></tr>
                            <tr><td>Confirmed Cases: </td><td style="text-align: right">` + getCases(d) + `</td></tr>
                            <tr><td>Confirmed Deaths: </td><td style="text-align: right">` + getDeaths(d) + `</td></tr>
                            <tr><td>Case Delta: </td><td style="text-align: right">` + getCaseDeltas(d) + `</td></tr>
                            <tr><td>Moving Average: </td><td style="text-align: right">` + getMovingAvg(d) + `</td></tr>
                    </table>
                    `;
            return content;
        });
    svg.call(tip);
}

function createLineLegend(svg, width, height, legend) {
    let size = 10;

    svg.selectAll(".legend-lines")
        .data(legend)
        .enter()
        .append("line")
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .attr("class", "legend-lines")
        .style("stroke", function (d) {
            return d.color;
        })
        .style("shape-rendering", "crispEdges")
        .style("stroke-width", 1)
        .attr("x1", function (d) {
            return d.x - 5;
        })
        .attr("y1", function (d, i) {
            return height + 85 + (i * (size + 5));
        })
        .attr("x2", function (d) {
            return d.x - 5 + size;
        })
        .attr("y2", function (d, i) {
            return height + 85 + (i * (size + 5));
        });

    let circleX = [];
    legend.forEach(function (d, i) {
        circleX.push({yOffset: i, x: d.x, color: d.color});
        circleX.push({yOffset: i, x: d.x + size, color: d.color});
    });

    svg.selectAll(".legend-circles")
        .data(circleX)
        .enter()
        .append('circle')
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .attr('class', "legend-circles")
        .attr('cx', function (d) {
            return d.x - 5;
        })
        .attr('cy', function (d, i) {
            return height + 85 + (d.yOffset * (size + 5));
        })
        //.attr("stroke", highlightColor)
        .style("fill", function (d) {
            return d.color;
        })
        .attr('r', 2);

    svg.selectAll("mylabels")
        .data(legend)
        .enter()
        .append("text")
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .attr("x", function (d) {
            return d.x + size + 5
        })
        .attr("y", function (d, i) {
            return height + 80 + (i * (size + 5)) + 9;
        })
        .style("fill", function (d) {
            return d.color;
        })
        .text(function (d) {
            return d.key;
        })
        .attr("text-anchor", "left")
        .style("font", "11px sans-serif")
        .style("alignment-baseline", "middle")
}

function createBoxLegend(svg, width, height, legend) {
    let size = 8
    svg.selectAll("mydots")
        .data(legend)
        .enter()
        .append("rect")
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .attr("x", function (d) {
            return d.x
        })
        .attr("y", function (d, i) {
            return getLegendHeight(height, size, i);
        })
        .attr("width", size)
        .attr("height", size)
        .style("fill", function (d) {
            return d.color;
        })


    svg.selectAll("mylabels")
        .data(legend)
        .enter()
        .append("text")
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .attr("x", function (d) {
            return d.x + size + 5
        })
        .attr("y", function (d, i) {
            return height + 80 + (i * (size + 7)) + 9;
        })
        .style("fill", function (d) {
            if (d.textColor) {
                return d.textColor;
            }

            return d.color;
        })
        .text(function (d) {
            return d.key;
        })
        .attr("text-anchor", "left")
        .style("font", "11px sans-serif")
        .style("alignment-baseline", "middle")
}

function createShapeLegend(svg, width, height, legend) {
    let size = 8;
    let triangle = d3.symbol()
        .type(d3.symbolDiamond)
        .size(20);

    svg.selectAll("myshapes")
        .data(legend)
        .enter()
        .append("path")
        //.attr("class", clazz)
        .attr("d", triangle)
        .attr("transform", function (d, i) {
            return "translate(" + (d.x + 5) + "," + (getLegendHeight(height, size, i) + 4) + ")";
        })
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .style("fill", function (d) {
            return d.color;
        })
        .style("stroke", function (d) {
            return d.color;
        });


    svg.selectAll("mylabels")
        .data(legend)
        .enter()
        .append("text")
        .on("click", toggleVisibility)
        .on("mouseover", function (d) {
            if (d.tooltip) {
                d.tooltip.show();
            }
        })
        .on("mouseout", function (d) {
            if (d.tooltip) {
                d.tooltip.hide();
            }
        })
        .attr("x", function (d) {
            return d.x + size + 5
        })
        .attr("y", function (d, i) {
            return height + 80 + (i * (size + 7)) + 9;
        })
        .style("fill", function (d) {
            if (d.textColor) {
                return d.textColor;
            }

            return d.color;
        })
        .text(function (d) {
            return d.key;
        })
        .attr("text-anchor", "left")
        .style("font", "11px sans-serif")
        .style("alignment-baseline", "middle")
}

function getLegendHeight(height, size, i) {
    return height + 81 + (i * ((size + 2) + 5))
}

function toggleVisibility(d) {
    let currentVis = d3.selectAll("." + d.clazz).style("visibility");
    console.log("Legend click! Toggling " + d.key + "... current visibility: " + currentVis);

    let targetVis = "visible";
    if (targetVis === currentVis) {
        targetVis = "hidden";
    }

    d3.selectAll("." + d.clazz).transition().duration(200).style("visibility", targetVis);
}

function calculateCorrelationCoefficient(xData, yData) {
    if (xData.length !== yData.length) {
        throw "In order to calculate standard deviation, x and y must have the same number of elements!";
    }

    let NaNCount = 0;
    for (let i = 0; i < xData.length; i++) {
        if (!isNaN(xData[i]) && !isNaN(yData[i])) {
            break;
        }

        if (isNaN(xData[i]) || isNaN(yData[i])) {
            NaNCount++;
        }
    }

    console.log("xData.length: " + xData.length + ", xData: " + xData);
    console.log("yData.length: " + yData.length + ", yData: " + yData);
    console.log(NaNCount + " NaN's found at the beginning of the array! yData.length: " + yData.length);
    for (let i = 0; i < NaNCount; i++) {
        xData.pop();
        yData.shift();
    }
    console.log("After shift - xData.length: " + xData.length + ", yData.length: " + yData.length);
    console.log("xData.length: " + xData.length + ", xData: " + xData);
    console.log("yData.length: " + yData.length + ", yData: " + yData);

    let xMean = xData.reduce((a, b) => a + b) / xData.length;
    console.log("xMean: " + xMean);
    let yMean = yData.reduce((a, b) => a + b) / yData.length;
    console.log("yMean: " + yMean);
    let xStdev = Math.sqrt(xData.reduce(function (sq, n) {
        return sq + Math.pow(n - xMean, 2);
    }, 0) / (xData.length - 1));
    console.log("xStdev: " + xStdev);
    let yStdev = Math.sqrt(yData.reduce(function (sq, n) {
        return sq + Math.pow(n - yMean, 2);
    }, 0) / (yData.length - 1));
    console.log("yStdev: " + yStdev);
    let xStandardized = xData.map(function (d) {
        return (d - xMean) / xStdev;
    });
    //console.log("xStandardized: " + xStandardized);
    let yStandardized = yData.map(function (d) {
        return (d - yMean) / yStdev;
    });
    //console.log("yStandardized: " + yStandardized);
    let multipliedCoords = [];

    for (let i = 0; i < xStandardized.length; i++) {
        multipliedCoords.push(xStandardized[i] * yStandardized[i]);
    }

    let correlationCoefficient = multipliedCoords.reduce((a, b) => a + b) / (xData.length - 1);

    console.log("Correlation Coefficient: " + correlationCoefficient);

    return correlationCoefficient;
}

d3.selection.prototype.moveToFront = function () {
    return this.each(function () {
        this.parentNode.appendChild(this);
    });
};