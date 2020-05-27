function updateBarChart(data, dataCallback, clazz, color, highlightColor) {
    var selectedData = dphSvg.selectAll("." + clazz).data(data);
    var enterData = selectedData.enter();

    enterData.append("rect")
        .attr("class", clazz)
        .attr("x", function (d) {
            return dphxScale(d.label);
        })
        .attr("y", function (d) {
            if (dataCallback(d) > 0) {
                return dphyScale(dataCallback(d));
            } else {
                return dphyScale(0);
            }
        })
        .attr("height", function (d) {
            return Math.abs(dphyScale(dataCallback(d)) - dphyScale(0));
        })
        .attr("width", dphxScale.bandwidth())
        .style("fill", color)
        .on("mouseover", function (d, i) {
            handleDeathMouseOver(d, i, d3.select(this), highlightColor)
        })
        .on("mouseout", function (d, i) {
            handleDeathMouseOut(d, i, d3.select(this), color)
        });

    selectedData.transition().duration(100)
        .attr("y", function (d) {
            if (dataCallback(d) > 0) {
                return dphyScale(dataCallback(d));
            } else {
                return dphyScale(0);
            }
        })
        .attr("height", function (d) {
            return Math.abs(dphyScale(dataCallback(d)) - dphyScale(0));
        });

    selectedData.exit().remove();

    // Ensure notable dates lines are on top!
    dphSvg.selectAll("." + clazz).moveToFront();
    dphSvg.selectAll("line").moveToFront();
}

function updateLineChart(data, dataCallback, clazz, color, highlightColor) {
    var selectedData = dphSvg.selectAll("." + clazz).data([data], function (d) {
        return d.label;
    });

    // Draw path
    selectedData.enter()
        .append("path")
        .attr("class", clazz)
        .attr("d", d3.line()
            //.curve(d3.curveCardinal)
            .x(function (d) {
                return dphxScale(d.label) + (dphxScale.bandwidth() / 2);
            })
            .y(function (d) {
                return dphyScale(dataCallback(d));
            })
            .defined(function (d) {
                return dataCallback(d) !== 0;
            }))
        //.merge(selectedData)
        .attr("fill", "none")
        .attr("stroke", color)
        .attr("stroke-width", 1);

    selectedData
        .merge(selectedData)
        .transition()
        .duration(100)
        .attr("d", d3.line()
            //.curve(d3.curveCardinal)
            .x(function (d) {
                return dphxScale(d.label) + (dphxScale.bandwidth() / 2);
            })
            .y(function (d) {
                return dphyScale(dataCallback(d));
            })
            .defined(function (d) {
                return dataCallback(d) !== 0;
            }))

    var circleClass = clazz + '-circle';
    var selectedCircles = dphSvg.selectAll('.' + circleClass).data(data);

    selectedCircles.enter()
        .append('circle')
        .attr('class', circleClass)
        .attr('cx', function (d) {
            return dphxScale(d.label) + (dphxScale.bandwidth() / 2);
        })
        .attr('cy', function (d) {
            return dphyScale(dataCallback(d));
        })
        .attr("stroke", highlightColor)
        .attr("fill", color)
        .attr('r', 2);

    selectedCircles
        .merge(selectedCircles)
        .transition()
        .duration(100)
        .attr('cx', function (d) {
            return dphxScale(d.label) + (dphxScale.bandwidth() / 2);
        })
        .attr('cy', function (d) {
            return dphyScale(dataCallback(d));
        })

    selectedCircles.exit().transition().duration(100).style("opacity", 0).remove();

    // Ensure notable dates lines are on top!
    dphSvg.selectAll("." + clazz).moveToFront();
    dphSvg.selectAll("line").moveToFront();
    dphSvg.selectAll('.' + circleClass).moveToFront();
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
                });
            });
    }

    return tempChartData;
}

function updateAllCharts(data, prelimDate) {
    console.log("Updating chart for prelim date " + prelimDate);

}

function make_y_gridlines() {
    return d3.axisLeft(dphyScale).tickValues([0, 100, 200, 300, 400, 500, 600, 700, 800, 900])
}

function drawAxisLines() {
    dphSvg.selectAll(".grid").remove();
    dphSvg.append("g")
        .attr("class", "grid")
        .call(make_y_gridlines()
            .tickSize(-width)
            .tickFormat("")
        );
}

function createXAxisTickValues() {
    let epicurve = getLastElement(chartData);
    let xAxisTickValues = [];

    for (let i = 0; i < epicurve.length; i += 2) {
        xAxisTickValues.push(epicurve[i].label);
    }

    return xAxisTickValues;
}

function createAxis(parent, xAxis, yAxis, height) {
    parent.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis.tickValues(createXAxisTickValues()))
        .selectAll("text")
        .style("text-anchor", "end")
        .attr("dx", "-.8em")
        .attr("dy", "-.55em")
        .attr("transform", "rotate(-90)");

    parent.append("g")
        .attr("class", "y axis")
        .call(yAxis)
        .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", 5)
        .attr("dy", ".71em")
        .style("text-anchor", "end")
        .text("Cases Per Day");
}

function getYScale(data) {
    if (deltaEnabled) {
        return 300;
    }

    return 100 + d3.max(data, function (d) {
        return d.cases;
    })
}

function createSlider() {
    let sliderTime = d3
        .sliderBottom()
        //.min(-1 * (timeData.length - 1))
        //.max(0)
        .domain([-42, 0])
        .marks([-42, 0])
        .step(1)
        //.ticks(5)
        .width(350)
        .tickValues([0, -7, -14, -21, -28, -35, -42]);

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
    d3This.style("fill", color);
    tip.show(d, i);
}

function handleDeathMouseOut(d, i, d3This, color) {
    d3This.style("fill", color);
    tip.hide(d, i);
}

function createTooltips(value) {
    tip = d3.tip().attr('class', 'd3-tip').direction('e').offset([0, 5])
        .html(function (d) {
            var content = "<span style='margin-left: 2.5px;'><b>" + d.label + "</b></span><br>";
            content += `
                    <table style="margin-top: 2.5px;">
                            <tr><td>Confirmed Cases: </td><td style="text-align: right">` + getCases(d) + `</td></tr>
                            <tr><td>Projected Cases: </td><td style="text-align: right">` + getCases(d) + `</td></tr>
                            <tr><td>Confirmed Deaths: </td><td style="text-align: right">` + getDeaths(d) + `</td></tr>
                    </table>
                    `;
            return content;
        });
    dphSvg.call(tip);
}

d3.selection.prototype.moveToFront = function () {
    return this.each(function () {
        this.parentNode.appendChild(this);
    });
};