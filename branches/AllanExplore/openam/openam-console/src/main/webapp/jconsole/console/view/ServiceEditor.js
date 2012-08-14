/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 16:59
 * To change this template use File | Settings | File Templates.
 */


Ext.define('AM.view.ServiceEditor', {
    extend:  'Ext.grid.Panel',
    alias:   'widget.serviceeditor',

    title: 'Service Configuration',
    store: 'ConfigStore',

    anchorSize: '92% 100%',

    selType: 'rowmodel',
    realmScope: 'global',

    initComponent: function() {
        this.columns        = [
            { header: 'Attribute',  dataIndex: 'name', flex: 1}   ,
            { header: 'Value',  dataIndex: 'value', flex: 4, editor: 'textfield'}
        ];
        this.plugins = [
            Ext.create('Ext.grid.plugin.RowEditing',{
                clicksToEdit: 1
            })
        ];

        console.log('Initializing ServiceEditor');
        this.callParent(arguments);
    },

    initServiceEditor: function(svc, realm) {
        Ext.Ajax.request({
            url: '../restconsole/services/'+svc+'/config/'+realm,
            scope: this,
            success: function(response, opts) {
                var obj = Ext.decode(response.responseText);
                var theData = [ ];
                if ((typeof obj != 'undefined')
                    && (typeof obj.config != 'undefined' )
                    &&(typeof obj.config.Attributes != 'undefined')) {
                    for (var x in obj.config.Attributes)  {
                        var xx = {name: x, value: obj.config.Attributes[x].toString()};
                        theData.push(xx);
                    }
                }
                opts.scope.store.loadData(theData) ;

                console.log('Updating new Data');
            },
            failure: function(response, opts) {
                console.log('server-side failure with status code ' + response.status);
            }
        });
    }
});
